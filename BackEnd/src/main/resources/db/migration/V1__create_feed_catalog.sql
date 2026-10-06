CREATE TABLE feeds (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       title VARCHAR(255) NOT NULL,
                       homepage_url VARCHAR(2048) NOT NULL,
                       feed_url VARCHAR(2048) NOT NULL,
                       feed_url_hash CHAR(64) NOT NULL,
                       description TEXT NULL,
                       language VARCHAR(32) NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       last_checked_at TIMESTAMP NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                           ON UPDATE CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_feeds_feed_url_hash UNIQUE (feed_url_hash)
);

CREATE TABLE tags (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      name VARCHAR(80) NOT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE feed_tags (
                           feed_id BIGINT NOT NULL,
                           tag_id BIGINT NOT NULL,
                           PRIMARY KEY (feed_id, tag_id),
                           CONSTRAINT fk_feed_tags_feed
                               FOREIGN KEY (feed_id) REFERENCES feeds(id)
                                   ON DELETE CASCADE,
                           CONSTRAINT fk_feed_tags_tag
                               FOREIGN KEY (tag_id) REFERENCES tags(id)
                                   ON DELETE CASCADE
);

CREATE INDEX idx_feeds_active ON feeds(active);
CREATE INDEX idx_tags_name ON tags(name);
CREATE INDEX idx_feed_tags_tag_id ON feed_tags(tag_id);