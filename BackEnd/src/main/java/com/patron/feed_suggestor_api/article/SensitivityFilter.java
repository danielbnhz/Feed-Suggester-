package com.patron.feed_suggestor_api.article;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SensitivityFilter {

    private final boolean enabled;
    private final int threshold;

    public SensitivityFilter(
            @Value("${filter.sensitivity.enabled:true}") boolean enabled,
            @Value("${filter.sensitivity.threshold:3}") int threshold) {
        this.enabled = enabled;
        this.threshold = threshold;
    }

    public boolean isEnabled() { return enabled; }
    public int getThreshold() { return threshold; }

    public int score(String title, String summary) {
        return enabled ? SensitivityClassifier.score(title, summary) : 0;
    }
}