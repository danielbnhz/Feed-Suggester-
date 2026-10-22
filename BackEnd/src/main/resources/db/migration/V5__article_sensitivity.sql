ALTER TABLE articles ADD COLUMN sensitivity_score INT NOT NULL DEFAULT 0;
ALTER TABLE articles ADD COLUMN sensitivity_checked_at TIMESTAMP;
CREATE INDEX idx_articles_sensitivity_score ON articles (sensitivity_score);