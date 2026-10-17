import { useState } from "react";
import ArticleCard from "./components/ArticleCard.tsx";

type Feed = {
    id: number;
    title: string;
    description: string | null;
    homepageUrl: string;
    language: string | null;
};

export default function App() {
    const [feeds, setFeeds] = useState<Feed[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function loadFeeds() {
        setLoading(true);
        setError(null);
        try {
            const response = await fetch("/api/feeds");
            if (!response.ok) {
                throw new Error(`Server responded with ${response.status}`);
            }
            const data: Feed[] = await response.json();
            setFeeds(data);
        } catch (e) {
            setError(e instanceof Error ? e.message : "Something went wrong");
        } finally {
            setLoading(false);
        }
    }

    const notice = error
        ? `Couldn't load: ${error}`
        : loading
            ? "Loading..."
            : feeds.length > 0
                ? `Showing ${feeds.length} sources. Article fetching comes next.`
                : "Discover something worth reading.";

    return (
        <main className="app-shell">
            <header className="hero">
                <h1 className="hero_header">Feed-Suggester</h1>

                <button
                    className="find-articles-button"
                    type="button"
                    onClick={loadFeeds}
                    disabled={loading}
                >
                    Find articles to read
                </button>

                <p className="hero_notice" role="status">
                    {notice}
                </p>
            </header>

            <section className="article-grid" aria-label="Suggested articles">
                {feeds.map((feed) => (
                    <ArticleCard
                        key={feed.id}
                        title={feed.title}
                        description={feed.description}
                        url={feed.homepageUrl}
                    />
                ))}
            </section>
        </main>
    );
}