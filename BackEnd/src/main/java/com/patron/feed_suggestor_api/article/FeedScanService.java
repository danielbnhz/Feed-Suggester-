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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class FeedScanService {

    private static final Logger log = LoggerFactory.getLogger(FeedScanService.class);

    private static final Duration MANUAL_COOLDOWN = Duration.ofMinutes(2);
    private static final int MAX_ENTRIES_PER_FEED = 30;
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_PAGE_FETCHES_PER_SCAN = 100;
    private final AtomicInteger pageFetchesThisScan = new AtomicInteger();
    private static final int MIN_USEFUL_SUMMARY_LENGTH = 20;
    private final SensitivityFilter sensitivityFilter;

    private static final Pattern META_TAG = Pattern.compile("(?is)<meta\\b[^>]*>");
    private static final Pattern ATTR_NAME = metaAttr("name");
    private static final Pattern ATTR_PROPERTY = metaAttr("property");
    private static final Pattern ATTR_CONTENT = metaAttr("content");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("(?i)&#(x[0-9a-f]+|[0-9]+);");


    private static final Pattern IMG_TAG = Pattern.compile("(?i)<img\\b[^>]*>");
    private static final Pattern SRC_ATTR = Pattern.compile("(?i)\\bsrc=[\"']([^\"']+)[\"']");
    private static final Pattern LAZY_SRC = Pattern.compile("(?i)\\bdata-(?:lazy-)?src=[\"']([^\"']+)[\"']");
    private static final Pattern SIZE_ONE = Pattern.compile("(?i)\\b(?:width|height)\\s*=\\s*[\"']?1[\"']?(?![0-9])");
    private static final Pattern JUNK_IMAGE = Pattern.compile("(?i)(pixel|tracking|spacer|1x1|gravatar|emoji|feedburner|doubleclick)");

    private static final Pattern OG_IMAGE = Pattern.compile(
            "(?i)<meta[^>]+(?:property|name)=[\"'](?:og:image(?::secure_url)?|twitter:image(?::src)?)[\"'][^>]*content=[\"']([^\"']+)[\"']"
                    + "|<meta[^>]+content=[\"']([^\"']+)[\"'][^>]*(?:property|name)=[\"'](?:og:image(?::secure_url)?|twitter:image(?::src)?)[\"']");

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

    public FeedScanService(FeedRepository feedRepository, ArticleRepository articleRepository,
                           SensitivityFilter sensitivityFilter) {
        this.feedRepository = feedRepository;
        this.articleRepository = articleRepository;
        this.sensitivityFilter = sensitivityFilter;

    }

    boolean isScanning() {
        return scanning.get();
    }

    private static String absolutize(String base, String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String abs = URI.create(base.trim()).resolve(url.trim().replace(" ", "%20")).toString();
            return abs.startsWith("http") ? limit(abs) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String allHtml(SyndEntry entry) {
        StringBuilder sb = new StringBuilder();
        SyndContent d = entry.getDescription();
        if (d != null && d.getValue() != null) sb.append(d.getValue());
        for (SyndContent c : entry.getContents()) {
            if (c.getValue() != null) sb.append(c.getValue());
        }
        return sb.toString();
    }

    private static String firstGoodImage(String html, String baseUrl) {
        if (html == null || html.isEmpty()) return null;
        Matcher tags = IMG_TAG.matcher(html);
        while (tags.find()) {
            String tag = tags.group();
            if (SIZE_ONE.matcher(tag).find()) continue;          // 1px tracking images
            Matcher lazy = LAZY_SRC.matcher(tag);
            Matcher src = SRC_ATTR.matcher(tag);
            String raw = lazy.find() ? lazy.group(1) : (src.find() ? src.group(1) : null);
            String abs = absolutize(baseUrl, raw == null ? null : raw.replace("&amp;", "&"));
            if (abs == null || JUNK_IMAGE.matcher(abs).find()) continue;
            return abs;
        }
        return null;
    }

    public String fetchOgImage(String pageUrl) {
        return fetchPageMeta(pageUrl).image();
    }

    public PageMeta fetchPageMeta(String pageUrl) {
        try {
            Thread.sleep(250);
            HttpRequest req = HttpRequest.newBuilder(URI.create(pageUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "FeedSuggester/0.1 (personal project)")
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            HttpResponse<InputStream> res =
                    httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() / 100 != 2) {
                res.body().close();
                return PageMeta.EMPTY;
            }
            try (InputStream in = res.body()) {
                String head = new String(in.readNBytes(150_000), StandardCharsets.UTF_8);

                String image = null;
                Matcher m = OG_IMAGE.matcher(head);
                if (m.find()) {
                    String raw = m.group(1) != null ? m.group(1) : m.group(2);
                    image = absolutize(pageUrl, raw.replace("&amp;", "&"));
                }
                return new PageMeta(image, metaDescription(head));
            }
        } catch (Exception e) {
            log.debug("page fetch failed for {}: {}", pageUrl, e.toString());
        }
        return PageMeta.EMPTY;
    }

    private static Pattern metaAttr(String attribute) {
        return Pattern.compile(
                "(?i)(?<![\\w-])" + attribute + "\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");
    }

    private static String attrValue(Pattern pattern, String tag) {
        Matcher m = pattern.matcher(tag);
        if (!m.find()) return null;
        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    private static String metaDescription(String head) {
        String fallback = null;
        Matcher tags = META_TAG.matcher(head);
        while (tags.find()) {
            String tag = tags.group();
            String key = attrValue(ATTR_PROPERTY, tag);
            if (key == null) key = attrValue(ATTR_NAME, tag);
            String content = attrValue(ATTR_CONTENT, tag);
            if (key == null || content == null || content.isBlank()) continue;

            key = key.toLowerCase();
            if (key.equals("og:description")) return content;
            if (fallback == null
                    && (key.equals("description") || key.equals("twitter:description"))) {
                fallback = content;
            }
        }
        return fallback;
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

    public record PageMeta(String image, String description) {
        static final PageMeta EMPTY = new PageMeta(null, null);
    }


    private void runScan() {
        pageFetchesThisScan.set(0);
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

        String image = extractImageUrl(entry);
        String summary = truncate(stripHtml(extractSummary(entry)), MAX_SUMMARY_LENGTH);

        boolean needsImage = image == null;
        boolean needsSummary = !isUsefulSummary(summary);

        if ((needsImage || needsSummary)
                && pageFetchesThisScan.incrementAndGet() <= MAX_PAGE_FETCHES_PER_SCAN) {
            PageMeta meta = fetchPageMeta(link);
            if (needsImage) image = meta.image();
            if (needsSummary) {
                summary = truncate(stripHtml(meta.description()), MAX_SUMMARY_LENGTH);
            }
        }
        if (!isUsefulSummary(summary)) summary = null;

        Article article = new Article(
                feed,
                truncate(title, 512),
                link,
                hash,
                summary,
                image,
                extractDate(entry)
        );
        if (sensitivityFilter.isEnabled()) {
            article.applySensitivity(sensitivityFilter.score(article.getTitle(), article.getSummary()));
        }
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

        return firstGoodImage(allHtml(entry), entry.getLink());
    }

    private static String limit(String url) {
        return (url == null || url.length() > 2048) ? null : url;
    }

    private static LocalDateTime extractDate(SyndEntry entry) {
        Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
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

    private static boolean isUsefulSummary(String s) {
        return s != null && s.length() >= MIN_USEFUL_SUMMARY_LENGTH;
    }

    private static String decodeNumericEntities(String s) {
        Matcher m = NUMERIC_ENTITY.matcher(s);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String num = m.group(1);
            String replacement = " ";
            try {
                int codePoint = (num.charAt(0) == 'x' || num.charAt(0) == 'X')
                        ? Integer.parseInt(num.substring(1), 16)
                        : Integer.parseInt(num);
                if (Character.isValidCodePoint(codePoint)) {
                    replacement = new String(Character.toChars(codePoint));
                }
            } catch (NumberFormatException ignored) {
                // leave the space
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String stripHtml(String s) {
        if (s == null) return null;
        String cleaned = decodeNumericEntities(s.replaceAll("(?s)<[^>]*>", " "))
                .replace('\u00A0', ' ')
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

}