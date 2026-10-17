package com.patron.feed_suggestor_api.feed;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feeds")
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "homepage_url", nullable = false, length = 2048)
    private String homepageUrl;

    @Column(name = "feed_url", nullable = false, length = 2048)
    private String feedUrl;

    @Column(name = "feed_url_hash", nullable = false, length = 64, unique = true)
    private String feedUrlHash;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 32)
    private String language;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Feed() {
    }

    public Feed(
            String title,
            String homepageUrl,
            String feedUrl,
            String feedUrlHash,
            String description,
            String language
    ) {
        this.title = title;
        this.homepageUrl = homepageUrl;
        this.feedUrl = feedUrl;
        this.feedUrlHash = feedUrlHash;
        this.description = description;
        this.language = language;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public String getFeedUrlHash() {
        return feedUrlHash;
    }

    public String getDescription() {
        return description;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void markChecked(LocalDateTime when) {
        this.lastCheckedAt = when;
    }
}