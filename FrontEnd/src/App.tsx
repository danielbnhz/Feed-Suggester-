import { useCallback, useEffect, useMemo, useState } from "react";
import ArticleCard from "./components/ArticleCard.tsx";
import About from "./components/About.tsx";

type Tab = "feed" | "about";

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


const POLL_INTERVAL_MS = 3000;
const POLL_ATTEMPTS = 8;

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export default function App() {
    const [articles, setArticles] = useState<Article[]>([]);
    const [feeds, setFeeds] = useState<Feed[]>([]);
    const [scanning, setScanning] = useState(false);
    const [notice, setNotice] = useState<string | null>(null);
    const [tab, setTab] = useState<Tab>("feed");
    const [showSources, setShowSources] = useState(false);
    const [safeMode, setSafeMode] = useState<boolean>(
        () => localStorage.getItem("safeMode") !== "false"
    );




    useEffect(() => {
        localStorage.setItem("safeMode", String(safeMode));
    }, [safeMode]);

    const sortedFeeds = useMemo(
        () => [...feeds].sort((a, b) => a.title.localeCompare(b.title)),
        [feeds]
    );

    const loadArticles = useCallback(async () => {
        const response = await fetch(`/api/articles?limit=60&safe=${safeMode}`);
        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }
        setArticles(await response.json());
    }, [safeMode]);

    const loadFeeds = useCallback(async () => {
        const response = await fetch("/api/feeds");
        if (!response.ok) {
            throw new Error(`Server responded with ${response.status}`);
        }
        setFeeds(await response.json());
    }, []);

    useEffect(() => {
        if (showSources && feeds.length === 0) {
            loadFeeds().catch(() => setNotice("Couldn't load sources."));
        }
    }, [showSources, feeds.length, loadFeeds]);


    useEffect(() => {
        loadArticles().catch((e) =>
            setNotice(`Couldn't load articles: ${e instanceof Error ? e.message : "unknown error"}`)
        );
    }, [loadArticles]);

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
            <nav className="topbar" aria-label="Sections">
                <div className="topbar__links">
                    <button type="button" className="topbar__link"
                            aria-current={tab === "feed" ? "page" : undefined}
                            onClick={() => { setTab("feed"); setShowSources(false); }}>Feed</button>
                    <button type="button" className="topbar__link"
                            aria-current={tab === "about" ? "page" : undefined}
                            onClick={() => setTab("about")}>About</button>
                </div>
                <button type="button" className="filter-toggle" aria-pressed={safeMode}
                        onClick={() => setSafeMode((v) => !v)}>
                    18+ filter: {safeMode ? "On" : "Off"}
                </button>
            </nav>
            <header className="hero">
                <div className="hero_brand">
                    <img
                        className="hero_logo"
                        src="/hero.webp"
                        alt=""
                        width={104}
                        height={104}
                        decoding="async"
                    />
                    <h1 className="hero_header">Feed-Suggester</h1>
                </div>

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

            </header>

            {tab === "feed" ? (
                <>
                    <button
                        type="button"
                        className="sources-toggle"
                        onClick={() => setShowSources((v) => !v)}
                    >
                        {showSources ? "Articles" : "Sources"}
                    </button>

                    {showSources ? (
                        sortedFeeds.length > 0 ? (
                            <div className="source-table-wrapper">
                                <table className="source-table">
                                    <thead>
                                    <tr>
                                        <th scope="col">Source</th>
                                        <th scope="col">Link</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {sortedFeeds.map((feed) => (
                                        <tr key={feed.id}>
                                            <td className="source-table__name">{feed.title}</td>
                                            <td>
                                                <a
                                                    className="source-table__link"
                                                    href={feed.homepageUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                >
                                                    Check source →
                                                </a>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <p>Loading sources…</p>
                        )
                    ) : (
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
                    )}
                </>
            ) : (
                <About />
            )}
        </main>
    );
}