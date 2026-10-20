package com.patron.feed_suggestor_api.article;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ImageBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ImageBackfillService.class);
    private static final int BATCH_SIZE = 25;

    private final ArticleRepository articleRepository;
    private final FeedScanService feedScanService;

    public ImageBackfillService(ArticleRepository articleRepository, FeedScanService feedScanService) {
        this.articleRepository = articleRepository;
        this.feedScanService = feedScanService;
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT10M")
    public void backfill() {
        if (feedScanService.isScanning()) return;

        List<ArticleRepository.ImageCandidate> batch =
                articleRepository.findNeedingImage(PageRequest.of(0, BATCH_SIZE));
        int found = 0;
        for (ArticleRepository.ImageCandidate c : batch) {
            String image = feedScanService.fetchOgImage(c.getUrl());
            articleRepository.recordImageCheck(c.getId(), image, LocalDateTime.now(ZoneOffset.UTC));
            if (image != null) found++;
        }
        if (!batch.isEmpty()) {
            log.info("Image backfill: {} of {} articles got an image", found, batch.size());
        }
    }
}