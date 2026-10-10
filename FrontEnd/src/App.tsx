import { useState } from "react";
import ArticleCard from "./components/ArticleCard.tsx";

const placeholderArticles = [
    {
        id: "placeholder-1",
        title: "Your first suggested article",
        description:
            "An article description or preview will appear here.",
    },
    {
        id: "placeholder-2",
        title: "Something worth exploring",
        description:
            "Each article will have its own reusable card component.",
    },
    {
        id: "placeholder-3",
        title: "Another interesting read",
        description:
            "Preview images and article links can be connected later.",
    },
];

export default function App() {
    const [showPreviewNotice, setShowPreviewNotice] = useState(false);

    return (
        <main className="app-shell">
            <header className="hero">
                <h1 className="hero_header">Feed-Suggester</h1>

                <button
                    className="find-articles-button"
                    type="button"
                    onClick={() => setShowPreviewNotice(true)}
                >
                    Find articles to read
                </button>

                <p className="hero_notice" role="status">
                    {showPreviewNotice
                        ? "Layout preview only — article fetching is not connected yet."
                        : "Discover something worth reading."}
                </p>
            </header>

            <section className="article-grid" aria-label="Suggested articles">
                {placeholderArticles.map((article) => (
                    <ArticleCard
                        key={article.id}
                        title={article.title}
                        description={article.description}
                    />
                ))}
            </section>
        </main>
    );
}