# Feed-Suggester

A personal reading tool that pulls articles from RSS feeds across tech, science, ideas, news, music, and games, then presents them in one clean, image-first feed. It's built to help me find things worth reading without depending on a recommendation algorithm I don't control.

**Stack:** TypeScript + React · Java + Spring Boot · PostgreSQL

## Features

- **One feed from many sources.** A seeded list of about 50 RSS/Atom feeds is scanned automatically, with new articles deduplicated by a SHA-256 hash of their URL.
- **Scheduled and manual scans.** A background job scans every 30 minutes. A "Find articles to read" button triggers a scan on demand, with a 2-minute cooldown so it can't hammer other people's servers.
- **Image extraction with fallbacks.** The scanner looks for an image in this order: Media RSS thumbnails and content, feed enclosures, the first usable `<img>` in the entry HTML (skipping tracking pixels), and finally the article page's own `og:image`. A separate backfill job retries articles that still have no image.
- **Description cleanup.** Aggregator feeds like Hacker News send "Comments" as the description. The scanner treats very short or missing summaries as junk, falls back to the article page's meta description, and shows "No description available." if the page has none.
- **Graceful empty states.** Articles with no image, or an image that fails to load, get a placeholder card instead of a broken-image icon.
- **Sources tab.** An alphabetical table of every feed the app reads, with a link out to each source's site.
- **Responsive layout.** The card grid adapts from one column on phones up to four on wide screens.
- **Polite fetching.** The scanner identifies itself with a User-Agent, caps entries per feed, rate-limits page fetches per scan, and applies timeouts to every request.

## Architecture

```
React + TypeScript (browser)
        │  fetch /api/...
        ▼
Spring Boot REST API ──► scheduled scan ──► RSS/Atom feeds (Rome)
        │                      │
        │                      └─► article pages (og:image, meta description)
        ▼
PostgreSQL  (schema managed by Flyway)
```

| Endpoint | Purpose |
|---|---|
| `GET /api/articles?limit=60` | Latest articles, newest first |
| `POST /api/articles/scan` | Start a scan; returns `429` with `retryAfterSeconds` during the cooldown |
| `GET /api/feeds` | List of configured sources |

**Backend:** Java, Spring Boot, Gradle, JPA/Hibernate (schema validated, not auto-generated), Flyway migrations, [Rome](https://rometools.github.io/rome/) for RSS/Atom parsing with the Media RSS module.

**Frontend:** React with TypeScript, plain CSS with theme variables.

## Running it locally

**Prerequisites:** a JDK, Node.js with npm, and PostgreSQL.

1. **Create the database.**

   ```sql
   CREATE ROLE feed_app WITH LOGIN PASSWORD 'choose-a-password';
   CREATE DATABASE feed_suggestor OWNER feed_app;
   ```

2. **Start the backend.** Set the password in your environment, then run the app from the backend directory. Flyway creates the tables and seeds the feeds on first start.

   ```bash
   export DB_PASSWORD=choose-a-password
   ./gradlew bootRun
   ```

3. **Start the frontend.** From the frontend directory:

   ```bash
   npm install
   npm run dev
   ```

The first scan runs shortly after the server starts. Articles appear as feeds are processed, and you can press **Find articles to read** to scan again.

## Adding sources

Feeds live in the database, seeded through Flyway migrations. To add more, create a new migration that inserts into `feeds` (title, homepage URL, feed URL, and the SHA-256 hash of the feed URL). The `ON CONFLICT` clause in the existing seeds makes re-running them safe.

## Roadmap

- **Preference engine.** Rank articles with a tag system and a lightweight learner that adapts to what I actually read, instead of a fixed order. The deployed app has to run on a free backend, so heavy ML stays out of the main app. Any heavier training would run separately and occasionally.
- **Categories and topic feeds.** Group articles by topic, with a master feed plus specialized feeds.
- **Retention cap.** Automatically delete old articles once storage reaches a size limit.
- **Description backfill.** Retry missing descriptions for older articles, as the image backfill already does for images.
- **Deployment.** Host it free for personal use behind a simple password.

## Status

Working end to end as a personal tool. The preference engine and categories are next.