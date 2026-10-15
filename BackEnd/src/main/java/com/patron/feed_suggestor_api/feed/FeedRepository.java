package com.patron.feed_suggestor_api.feed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {

    List<Feed> findByActiveTrueOrderByTitleAsc();
}