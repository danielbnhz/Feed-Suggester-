type ArticleCardProps = {
    title: string;
    description?: string | null;
    imageUrl?: string | null;
    url?: string | null;
};

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
        {description || "No description available."}
    </p>

    {url ? (
        <a className="article-card__link" href={url}>
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