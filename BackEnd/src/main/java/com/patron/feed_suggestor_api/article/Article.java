package com.patron.feed_suggestor_api.article;

import com.patron.feed_suggestor_api.feed.Feed;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "url_hash", nullable = false, length = 64, unique = true)
    private String urlHash;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "fetched_at", insertable = false, updatable = false)
    private LocalDateTime fetchedAt;

    protected Article() {
    }

    public Article(Feed feed, String title, String url, String urlHash,
                   String summary, String imageUrl, LocalDateTime publishedAt) {
        this.feed = feed;
        this.title = title;
        this.url = url;
        this.urlHash = urlHash;
        this.summary = summary;
        this.imageUrl = imageUrl;
        this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public Feed getFeed() { return feed; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getUrlHash() { return urlHash; }
    public String getSummary() { return summary; }
    public String getImageUrl() { return imageUrl; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}