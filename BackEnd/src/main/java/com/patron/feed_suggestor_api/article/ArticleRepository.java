package com.patron.feed_suggestor_api.article;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrlHash(String urlHash);

    @Query("select a from Article a join fetch a.feed "
            + "order by coalesce(a.publishedAt, a.fetchedAt) desc")
    List<Article> findLatest(Pageable pageable);
}