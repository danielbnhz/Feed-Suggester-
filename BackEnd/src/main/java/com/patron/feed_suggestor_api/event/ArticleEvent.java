package com.patron.feed_suggestor_api.event;

import com.patron.feed_suggestor_api.article.Article;
import com.patron.feed_suggestor_api.feed.Feed;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "article_events")
public class ArticleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ArticleEvent() {}

    public ArticleEvent(UUID clientId, Article article, EventType eventType) {
        this.clientId = clientId;
        this.article = article;
        this.feed = article.getFeed();
        this.eventType = eventType;
    }

    public Long getId() { return id; }
    public UUID getClientId() { return clientId; }
    public Article getArticle() { return article; }
    public Feed getFeed() { return feed; }
    public EventType getEventType() { return eventType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}