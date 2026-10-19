package com.patron.feed_suggestor_api.article;

import com.patron.feed_suggestor_api.feed.Feed;
import com.patron.feed_suggestor_api.feed.FeedRepository;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.modules.mediarss.types.UrlReference;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FeedScanService {

    private static final Logger log = LoggerFactory.getLogger(FeedScanService.class);

    private static final Duration MANUAL_COOLDOWN = Duration.ofMinutes(2);
    private static final int MAX_ENTRIES_PER_FEED = 30;
    private static final int MAX_SUMMARY_LENGTH = 500;

    public enum Outcome { STARTED, ALREADY_RUNNING, COOLING_DOWN }

    public record ScanRequestResult(Outcome outcome, long retryAfterSeconds) {}

    private final FeedRepository feedRepository;
    private final ArticleRepository articleRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private volatile Instant lastScanFinishedAt = Instant.EPOCH;

    public FeedScanService(FeedRepository feedRepository, ArticleRepository articleRepository) {
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
    }


    @Scheduled(initialDelayString = "PT10S", fixedDelayString = "PT30M")
    public void scheduledScan() {
        if (scanning.compareAndSet(false, true)) {
            runScan();
        }
    }


    public ScanRequestResult requestManualScan() {
        if (scanning.get()) {
            return new ScanRequestResult(Outcome.ALREADY_RUNNING, 0);
        }

        Duration sinceLast = Duration.between(lastScanFinishedAt, Instant.now());
        if (sinceLast.compareTo(MANUAL_COOLDOWN) < 0) {
            long wait = MANUAL_COOLDOWN.minus(sinceLast).toSeconds() + 1;
            return new ScanRequestResult(Outcome.COOLING_DOWN, wait);
        }

        if (!scanning.compareAndSet(false, true)) {
            return new ScanRequestResult(Outcome.ALREADY_RUNNING, 0);
        }
        CompletableFuture.runAsync(this::runScan);
        return new ScanRequestResult(Outcome.STARTED, 0);
    }

    private void runScan() {
        try {
            List<Feed> feeds = feedRepository.findByActiveTrueOrderByTitleAsc();
            int total = 0;
            for (Feed feed : feeds) {
                try {
                    int added = scanFeed(feed);
                    total += added;
                    log.info("Scanned '{}': {} new articles", feed.getTitle(), added);
                } catch (Exception e) {
                    log.warn("Failed to scan '{}' ({}): {}",
                            feed.getTitle(), feed.getFeedUrl(), e.toString());
                }
            }
            log.info("Scan complete: {} new articles from {} feeds", total, feeds.size());
        } finally {
            lastScanFinishedAt = Instant.now();
            scanning.set(false);
        }
    }

    private int scanFeed(Feed feed) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(feed.getFeedUrl()))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "FeedSuggester/0.1 (personal project)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() / 100 != 2) {
            response.body().close();
            throw new IOException("HTTP " + response.statusCode());
        }

        int added = 0;
        try (InputStream in = response.body(); XmlReader reader = new XmlReader(in)) {
            SyndFeed syndFeed = new SyndFeedInput().build(reader);

            int seen = 0;
            for (SyndEntry entry : syndFeed.getEntries()) {
                if (seen++ >= MAX_ENTRIES_PER_FEED) break;
                if (saveEntryIfNew(feed, entry)) added++;
            }
        }

        feed.markChecked(LocalDateTime.now());
        feedRepository.save(feed);
        return added;
    }

    private boolean saveEntryIfNew(Feed feed, SyndEntry entry) {
        String link = entry.getLink();
        String title = stripHtml(entry.getTitle());
        if (link == null || link.isBlank() || title == null) return false;

        link = link.trim();
        if (link.length() > 2048) return false;

        String hash = sha256(link);
        if (articleRepository.existsByUrlHash(hash)) return false;

        Article article = new Article(
                feed,
                truncate(title, 512),
                link,
                hash,
                truncate(stripHtml(extractSummary(entry)), MAX_SUMMARY_LENGTH),
                extractImageUrl(entry),
                extractDate(entry)
        );
        articleRepository.save(article);
        return true;
    }

    private static String extractSummary(SyndEntry entry) {
        SyndContent description = entry.getDescription();
        if (description != null && description.getValue() != null) {
            return description.getValue();
        }
        if (!entry.getContents().isEmpty()) {
            return entry.getContents().get(0).getValue();
        }
        return null;
    }
    private static final java.util.regex.Pattern IMG_SRC =
            java.util.regex.Pattern.compile("(?i)<img[^>]+src=[\"']([^\"']+)[\"']");

    private static String extractImageUrl(SyndEntry entry) {

        com.rometools.rome.feed.module.Module module = entry.getModule(MediaModule.URI);
        if (module instanceof MediaEntryModule media) {
            if (media.getMetadata() != null) {
                Thumbnail[] thumbs = media.getMetadata().getThumbnail();
                if (thumbs != null && thumbs.length > 0 && thumbs[0].getUrl() != null) {
                    return limit(thumbs[0].getUrl().toString());
                }
            }
            for (MediaContent content : media.getMediaContents()) {
                boolean isImage = "image".equalsIgnoreCase(content.getMedium())
                        || (content.getType() != null && content.getType().startsWith("image/"));
                if (isImage && content.getReference() instanceof UrlReference ref) {
                    return limit(ref.getUrl().toString());
                }
                if (content.getMetadata() != null) {
                    Thumbnail[] t = content.getMetadata().getThumbnail();
                    if (t != null && t.length > 0 && t[0].getUrl() != null) {
                        return limit(t[0].getUrl().toString());
                    }
                }
            }
        }


        for (SyndEnclosure enclosure : entry.getEnclosures()) {
            String type = enclosure.getType();
            if (type != null && type.startsWith("image/")) {
                return limit(enclosure.getUrl());
            }
        }


        String html = extractSummary(entry);
        if (html != null) {
            java.util.regex.Matcher m = IMG_SRC.matcher(html);
            if (m.find() && m.group(1).startsWith("http")) {
                return limit(m.group(1));
            }
        }
        return null;
    }

    private static String limit(String url) {
        return (url == null || url.length() > 2048) ? null : url;
    }
    private static LocalDateTime extractDate(SyndEntry entry) {
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    private static String stripHtml(String s) {
        if (s == null) return null;
        String cleaned = s.replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}