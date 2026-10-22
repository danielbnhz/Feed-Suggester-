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
public class SensitivityBackfillService {

    private static final Logger log = LoggerFactory.getLogger(SensitivityBackfillService.class);
    private static final int BATCH_SIZE = 200;

    private final ArticleRepository articleRepository;
    private final SensitivityFilter filter;

    public SensitivityBackfillService(ArticleRepository articleRepository, SensitivityFilter filter) {
        this.articleRepository = articleRepository;
        this.filter = filter;
    }

    @Scheduled(initialDelayString = "PT15S", fixedDelayString = "PT10M")
    public void backfill() {
        if (!filter.isEnabled()) return;
        int total = 0;
        List<ArticleRepository.SensitivityCandidate> batch;
        while (!(batch = articleRepository.findNeedingSensitivity(PageRequest.of(0, BATCH_SIZE))).isEmpty()) {
            for (ArticleRepository.SensitivityCandidate c : batch) {
                articleRepository.recordSensitivity(c.getId(),
                        filter.score(c.getTitle(), c.getSummary()),
                        LocalDateTime.now(ZoneOffset.UTC));
            }
            total += batch.size();
        }
        if (total > 0) log.info("Sensitivity backfill: scored {} articles", total);
    }
}