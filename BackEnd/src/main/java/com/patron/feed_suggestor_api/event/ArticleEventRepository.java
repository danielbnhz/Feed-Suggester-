package com.patron.feed_suggestor_api.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleEventRepository extends JpaRepository<ArticleEvent, Long> {
}