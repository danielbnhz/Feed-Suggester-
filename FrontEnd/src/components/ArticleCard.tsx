import { useState } from "react";

type ArticleCardProps = {
    title: string;
    description?: string | null;
    imageUrl?: string | null;
    url?: string | null;
};

const PREVIEW_MAX_CHARS = 80;
const PLACEHOLDER_EMOJI = "😕";

function clampText(text: string, max: number): string {
    if (text.length <= max) return text;
    const cut = text.slice(0, max - 1);
    const lastSpace = cut.lastIndexOf(" ");

    const base = lastSpace > max * 0.5 ? cut.slice(0, lastSpace) : cut;
    return base.trimEnd() + "…";
}

export default function ArticleCard({
                                        title,
                                        description,
                                        imageUrl,
                                        url,
                                    }: ArticleCardProps) {
    const [imageFailed, setImageFailed] = useState(false);

    const hasDescription =
        !!description && description.trim().toLowerCase() !== "comments";

    return (
        <article className="article-card">
            {imageUrl && !imageFailed ? (
                <img
                    className="article-card__image"
                    src={imageUrl}
                    alt=""
                    loading="lazy"
                    onError={() => setImageFailed(true)}
                />
            ) : (
                <div className="article-card__placeholder">
                    <span className="article-card__placeholder-icon" aria-hidden="true">
                        {PLACEHOLDER_EMOJI}
                    </span>
                    <span>No preview image</span>
                </div>
            )}

            <div className="article-card__content">
                <h2 className="article-card__title">{title}</h2>

                <p className="article-card__description">
                    {hasDescription
                        ? clampText(description!, PREVIEW_MAX_CHARS)
                        : "No description available."}
                </p>

                {url ? (
                    <a
                        className="article-card__link"
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        Read article →
                    </a>
                ) : (
                    <span className="article-card__pending">
                        Article link will appear here
                    </span>
                )}
            </div>
        </article>
    );
}