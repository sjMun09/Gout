CREATE TABLE post_hashtags (
  id         BIGSERIAL    PRIMARY KEY,
  post_id    UUID         NOT NULL,
  tag        VARCHAR(50)  NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT now(),
  CONSTRAINT uq_post_hashtag UNIQUE (post_id, tag)
);

CREATE INDEX idx_post_hashtags_tag     ON post_hashtags(tag);
CREATE INDEX idx_post_hashtags_post_id ON post_hashtags(post_id);
