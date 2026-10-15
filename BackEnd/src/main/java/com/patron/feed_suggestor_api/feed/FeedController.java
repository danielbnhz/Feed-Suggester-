package com.patron.feed_suggestor_api.feed;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feeds")
public class FeedController {

    private final FeedRepository feedRepository;

    public FeedController(FeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    @GetMapping
    public List<Feed> getActiveFeeds() {
        return feedRepository.findByActiveTrueOrderByTitleAsc();
    }
}