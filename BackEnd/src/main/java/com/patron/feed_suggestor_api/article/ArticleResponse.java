package com.patron.feed_suggestor_api.article;

import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String title,
        String url,
        String summary,
        String imageUrl,
        LocalDateTime publishedAt,
        String feedTitle
) {
    public static ArticleResponse from(Article a) {
        return new ArticleResponse(
                a.getId(),
                a.getTitle(),
                a.getUrl(),
                a.getSummary(),
                a.getImageUrl(),
                a.getPublishedAt(),
                a.getFeed().getTitle()
        );
    }
}