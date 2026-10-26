package com.patron.feed_suggestor_api.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ArticleEventRepository extends JpaRepository<ArticleEvent, Long> {
    boolean existsByClientIdAndArticle_IdAndEventType(
            UUID clientId,
            Long articleId,
            EventType eventType
    );
}