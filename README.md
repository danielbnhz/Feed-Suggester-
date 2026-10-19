# Feed-Suggester

A personal web app for discovering things worth reading. It pulls articles from a curated catalog of RSS/Atom feeds, stores them, and shows them as a browsable grid of cards. A "Find articles to read" button triggers a fresh scan on demand, and a Sources tab lists every feed in the catalog.

The long-term goal is personalized suggestions: a neural-net module that learns from what I actually click and read.

## Stack

| Layer | Technology |
| --- | --- |
| Frontend | React + TypeScript |
| Backend | Spring Boot 4.1 (Java 21), Spring Data JPA, Gradle |
| Feed parsing | [Rome](https://rometools.github.io/rome/) |
| Database | PostgreSQL |
| Migrations | Flyway (schema and seed feeds are applied automatically on startup) |

## How it works

- A scheduled job scans every active feed 10 seconds after startup and every 30 minutes afterward.
- Each scan reads up to 30 entries per feed, strips HTML from titles and summaries, and keeps summaries to 500 characters.
- Articles are deduplicated by a SHA-256 hash of their URL, so rescanning never creates duplicates.
- A manual scan (the button in the UI) runs in the background and has a 2-minute cooldown. The frontend polls for new articles while the scan runs.
- Feeds that fail to load are logged and skipped; one broken feed never stops a scan.

## API

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/articles?limit=50` | Latest articles, newest first (limit is clamped to 1-100) |
| `POST` | `/api/articles/scan` | Starts a background scan. Returns `202` (`started` or `already_running`) or `429` with a `Retry-After` header during cooldown |
| `GET` | `/api/feeds` | All active feeds |

## Running it locally

### 1. Database

Install PostgreSQL, then create a role and database:

```sql
CREATE USER feed_app WITH PASSWORD 'choose-a-password';
CREATE DATABASE feed_suggestor OWNER feed_app;
```

### 2. Backend

From the folder containing `build.gradle`:

```powershell
$env:DB_PASSWORD="choose-a-password"
.\gradlew.bat bootRun
```

On macOS or Linux:

```bash
DB_PASSWORD=choose-a-password ./gradlew bootRun
```

On first start, Flyway creates the tables and loads the starter feed list. The API listens on port 8080.

### 3. Frontend

From the frontend folder:

```bash
npm install
npm run dev
```

The frontend calls the API with relative paths (`/api/...`), so the dev server needs to proxy `/api` to `http://localhost:8080`.

## Configuration

| Setting | Where | Default |
| --- | --- | --- |
| Database URL | `spring.datasource.url` | `jdbc:postgresql://127.0.0.1:5432/feed_suggestor` |
| Database user | `spring.datasource.username` | `feed_app` |
| Database password | `DB_PASSWORD` environment variable | none (required) |
| Server port | `server.port` | `8080` |

Hibernate runs with `ddl-auto=validate`, so the database schema is owned entirely by the Flyway migrations in `src/main/resources/db/migration/`.

## Adding a feed

Add a new migration (for example `V3__add_feed.sql`) so the change is repeatable:

```sql
INSERT INTO feeds (title, homepage_url, feed_url, feed_url_hash, language)
VALUES (
  'Example Blog',
  'https://example.com',
  'https://example.com/feed.xml',
  encode(sha256(convert_to('https://example.com/feed.xml', 'UTF8')), 'hex'),
  'en'
)
ON CONFLICT (feed_url_hash) DO NOTHING;
```

## Roadmap

- [ ] Read `media:thumbnail` / `media:content` so more articles get preview images
- [ ] Feed-level fallback images and generated placeholders
- [ ] Record per-feed fetch status so dead feeds are easy to spot
- [ ] Track clicks and reads
- [ ] Neural-net module for personalized suggestions
- [ ] Docker Compose setup for one-command deployment

## Project layout

```
Feed-Suggester-/
├── BackEnd/      Spring Boot API (Gradle)
└── <frontend>/   React + TypeScript app
```
