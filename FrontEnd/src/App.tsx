import { useCallback, useEffect, useState } from "react";
import ArticleCard from "./components/ArticleCard.tsx";

type Article = {
    id: number;
    title: string;
    url: string;
    summary: string | null;
    imageUrl: string | null;
    publishedAt: string | null;
    feedTitle: string;
};

type Feed = {
    id: number;
    title: string;
    description: string | null;
    homepageUrl: string;
};

type Tab = "articles" | "sources";

const POLL_INTERVAL_MS = 3000;
const POLL_ATTEMPTS = 8;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export default function App() {
    const [tab, setTab] = useState<Tab>("articles");
    const [articles, setArticles] = useState<Article[]>([]);
    const [feeds, setFeeds] = useState<Feed[]>([]);
    const [scanning, setScanning] = useState(false);
    const [notice, setNotice] = useState<string | null>(null);

    const loadArticles = useCallback(async () => {
        const response = await fetch("/api/articles?limit=60");
        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }
        setArticles(await response.json());
    }, []);

    const loadFeeds = useCallback(async () => {
        const response = await fetch("/api/feeds");
        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }
        setFeeds(await response.json());
    }, []);

    // Show whatever is already saved when the page opens.
    useEffect(() => {
        loadArticles().catch((e) =>
            setNotice(`Couldn't load articles: ${e instanceof Error ? e.message : "unknown error"}`)
        );
    }, [loadArticles]);

    // Load sources the first time that tab is opened.
    useEffect(() => {
        if (tab === "sources" && feeds.length === 0) {
            loadFeeds().catch(() => setNotice("Couldn't load sources."));
        }
    }, [tab, feeds.length, loadFeeds]);

    async function findArticles() {
        setScanning(true);
        setNotice("Looking for new articles...");
        try {
            const response = await fetch("/api/articles/scan", { method: "POST" });

            if (response.status === 429) {
                const body = await response.json();
                setNotice(
                    `Scanned recently. Try again in about ${body.retryAfterSeconds}s. Showing what's saved.`
                );
                await loadArticles();
                return;
            }
            if (!response.ok) {
                throw new Error(`Server responded with ${response.status}`);
            }

            // The scan runs in the background, so refresh a few times while it works.
            for (let i = 0; i < POLL_ATTEMPTS; i++) {
                await sleep(POLL_INTERVAL_MS);
                await loadArticles();
            }
            setNotice("Up to date.");
        } catch (e) {
            setNotice(`Something went wrong: ${e instanceof Error ? e.message : "unknown error"}`);
        } finally {
            setScanning(false);
        }
    }

    return (
        <main className="app-shell">
            <header className="hero">
                <h1 className="hero_header">Feed-Suggester</h1>

                <button
                    className="find-articles-button"
                    type="button"
                    onClick={findArticles}
                    disabled={scanning}
                >
                    {scanning ? "Searching..." : "Find articles to read"}
                </button>

                <p className="hero_notice" role="status">
                    {notice ?? "Discover something worth reading."}
                </p>

                <nav aria-label="Sections">
                    <button type="button" onClick={() => setTab("articles")} disabled={tab === "articles"}>
                        Articles
                    </button>
                    <button type="button" onClick={() => setTab("sources")} disabled={tab === "sources"}>
                        Sources
                    </button>
                </nav>
            </header>

            {tab === "articles" ? (
                <section className="article-grid" aria-label="Suggested articles">
                    {articles.length === 0 && !scanning && (
                        <p>No articles yet. The first scan runs shortly after the server starts, or press the button.</p>
                    )}
                    {articles.map((article) => (
                        <ArticleCard
                            key={article.id}
                            title={article.title}
                            description={article.summary}
                            imageUrl={article.imageUrl}
                            url={article.url}
                        />
                    ))}
                </section>
            ) : (
                <section className="article-grid" aria-label="Sources">
                    {feeds.map((feed) => (
                        <ArticleCard
                            key={feed.id}
                            title={feed.title}
                            description={feed.description}
                            url={feed.homepageUrl}
                        />
                    ))}
                </section>
            )}
        </main>
    );
}