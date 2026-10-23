package com.patron.feed_suggestor_api.event;

import com.patron.feed_suggestor_api.article.Article;
import com.patron.feed_suggestor_api.article.ArticleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    public record EventRequest(Long articleId, EventType type) {}

    private final ArticleRepository articleRepository;
    private final ArticleEventRepository eventRepository;
    private final ClientIdResolver clientIdResolver;

    public EventController(ArticleRepository articleRepository,
                           ArticleEventRepository eventRepository,
                           ClientIdResolver clientIdResolver) {
        this.articleRepository = articleRepository;
        this.eventRepository = eventRepository;
        this.clientIdResolver = clientIdResolver;
    }

    @PostMapping
    public ResponseEntity<Void> record(@RequestBody EventRequest req,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        if (req.articleId() == null || req.type() == null) {
            return ResponseEntity.badRequest().build();
        }
        Article article = articleRepository.findById(req.articleId()).orElse(null);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        UUID clientId = clientIdResolver.resolve(request, response);
        eventRepository.save(new ArticleEvent(clientId, article, req.type()));
        return ResponseEntity.noContent().build();
    }
}