package com.patron.feed_suggestor_api.article;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsByUrlHash(String urlHash);

    @Query("select a from Article a join fetch a.feed "
            + "order by coalesce(a.publishedAt, a.fetchedAt) desc")
    List<Article> findLatest(Pageable pageable);

    interface ImageCandidate {
        Long getId();
        String getUrl();
    }

    @Query("select a.id as id, a.url as url from Article a "
            + "where a.imageUrl is null and a.imageCheckedAt is null "
            + "order by coalesce(a.publishedAt, a.fetchedAt) desc")
    List<ImageCandidate> findNeedingImage(Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Article a set a.imageUrl = :imageUrl, a.imageCheckedAt = :checkedAt where a.id = :id")
    int recordImageCheck(@Param("id") Long id,
                         @Param("imageUrl") String imageUrl,
                         @Param("checkedAt") LocalDateTime checkedAt);


    @Query("select a from Article a join fetch a.feed "
            + "where a.sensitivityScore < :threshold "
            + "order by coalesce(a.publishedAt, a.fetchedAt) desc")
    List<Article> findLatestSafe(@Param("threshold") int threshold, Pageable pageable);

    interface SensitivityCandidate {
        Long getId();
        String getTitle();
        String getSummary();
    }

    @Query("select a.id as id, a.title as title, a.summary as summary from Article a "
            + "where a.sensitivityCheckedAt is null order by a.id")
    List<SensitivityCandidate> findNeedingSensitivity(Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Article a set a.sensitivityScore = :score, a.sensitivityCheckedAt = :checkedAt where a.id = :id")
    int recordSensitivity(@Param("id") Long id,
                          @Param("score") int score,
                          @Param("checkedAt") LocalDateTime checkedAt);
}