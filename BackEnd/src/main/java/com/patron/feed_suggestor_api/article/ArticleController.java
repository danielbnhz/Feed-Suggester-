package com.patron.feed_suggestor_api.article;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final FeedScanService scanService;

    public ArticleController(ArticleRepository articleRepository, FeedScanService scanService) {
        this.articleRepository = articleRepository;
        this.scanService = scanService;
    }

    private final SensitivityFilter sensitivityFilter;

    public ArticleController(ArticleRepository articleRepository, FeedScanService scanService,
                             SensitivityFilter sensitivityFilter) {
        this.articleRepository = articleRepository;
        this.scanService = scanService;
        this.sensitivityFilter = sensitivityFilter;
    }

    @GetMapping
    public List<ArticleResponse> latest(@RequestParam(defaultValue = "50") int limit,
                                        @RequestParam(defaultValue = "true") boolean safe) {
        int size = Math.min(Math.max(limit, 1), 100);
        PageRequest page = PageRequest.of(0, size);
        List<Article> articles = (safe && sensitivityFilter.isEnabled())
                ? articleRepository.findLatestSafe(sensitivityFilter.getThreshold(), page)
                : articleRepository.findLatest(page);
        return articles.stream().map(ArticleResponse::from).toList();
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scan() {
        FeedScanService.ScanRequestResult result = scanService.requestManualScan();

        return switch (result.outcome()) {
            case STARTED -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "started"));
            case ALREADY_RUNNING -> ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "already_running"));
            case COOLING_DOWN -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()))
                    .body(Map.of("status", "cooling_down",
                            "retryAfterSeconds", result.retryAfterSeconds()));
        };
    }
}