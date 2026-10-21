type ArticleCardProps = {
    title: string;
    description?: string | null;
    imageUrl?: string | null;
    url?: string | null;
};

const PREVIEW_MAX_CHARS = 40;

function clampText(text: string, max: number): string {
    if (text.length <= max) return text;
    const cut = text.slice(0, max - 1);
    const lastSpace = cut.lastIndexOf(" ");
    // Prefer cutting at a word boundary, unless that would lose too much text.
    const base = lastSpace > max * 0.5 ? cut.slice(0, lastSpace) : cut;
    return base.trimEnd() + "…";
}


export default function ArticleCard({
                                        title,
                                        description,
                                        imageUrl,
                                        url,
                                    }: ArticleCardProps) {
    return (
        <article className="article-card">
            {imageUrl ? (
                        <img
                            className="article-card__image"
                    src={imageUrl}
                alt=""
                loading="lazy"
                    />
    ) : (
        <div className="article-card__placeholder">
            No preview image
    </div>
)}

    <div className="article-card__content">
    <h2 className="article-card__title">{title}</h2>

        <p className="article-card__description">
            {description
                ? clampText(description, PREVIEW_MAX_CHARS)
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