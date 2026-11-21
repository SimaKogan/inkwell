CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    bio           VARCHAR(500),
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_username ON users (lower(username));
CREATE UNIQUE INDEX ux_users_email    ON users (lower(email));

CREATE TABLE author (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL
);
CREATE UNIQUE INDEX ux_author_name ON author (lower(name));

CREATE TABLE catalog_item (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    category       VARCHAR(80)  NOT NULL,
    isbn           VARCHAR(20),
    price          NUMERIC(10, 2),
    stock          INTEGER      NOT NULL DEFAULT 0,
    published_year INTEGER,
    owner_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- search columns: not mapped by JPA, maintained by the database / EmbeddingService
    tsv            TSVECTOR GENERATED ALWAYS AS (
                       to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(description, '') || ' ' || coalesce(category, ''))
                   ) STORED,
    embedding      VECTOR(384)
);
CREATE UNIQUE INDEX ux_catalog_isbn     ON catalog_item (isbn) WHERE isbn IS NOT NULL;
CREATE INDEX ix_catalog_category        ON catalog_item (category);
CREATE INDEX ix_catalog_created         ON catalog_item (created_at DESC);
CREATE INDEX ix_catalog_tsv             ON catalog_item USING gin (tsv);
CREATE INDEX ix_catalog_title_trgm      ON catalog_item USING gin (title gin_trgm_ops);
CREATE INDEX ix_catalog_embedding       ON catalog_item USING hnsw (embedding vector_cosine_ops);

CREATE TABLE catalog_item_author (
    item_id   BIGINT NOT NULL REFERENCES catalog_item (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES author (id) ON DELETE CASCADE,
    PRIMARY KEY (item_id, author_id)
);

CREATE TABLE post (
    id              BIGSERIAL PRIMARY KEY,
    author_id       BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    catalog_item_id BIGINT      REFERENCES catalog_item (id) ON DELETE SET NULL,
    title           VARCHAR(200),
    body            TEXT        NOT NULL,
    source          VARCHAR(16) NOT NULL DEFAULT 'TYPED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    tsv             TSVECTOR GENERATED ALWAYS AS (
                        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(body, ''))
                    ) STORED,
    embedding       VECTOR(384)
);
CREATE INDEX ix_post_created      ON post (created_at DESC);
CREATE INDEX ix_post_author       ON post (author_id, created_at DESC);
CREATE INDEX ix_post_item         ON post (catalog_item_id);
CREATE INDEX ix_post_tsv          ON post USING gin (tsv);
CREATE INDEX ix_post_title_trgm   ON post USING gin (title gin_trgm_ops);
CREATE INDEX ix_post_embedding    ON post USING hnsw (embedding vector_cosine_ops);

CREATE TABLE comment (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    parent_id  BIGINT      REFERENCES comment (id) ON DELETE CASCADE,
    body       VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_comment_post ON comment (post_id, created_at);

CREATE TABLE post_like (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES post (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_like UNIQUE (post_id, user_id)
);

CREATE TABLE follow (
    id          BIGSERIAL PRIMARY KEY,
    follower_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    followee_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_follow UNIQUE (follower_id, followee_id),
    CONSTRAINT ck_follow_self CHECK (follower_id <> followee_id)
);
CREATE INDEX ix_follow_followee ON follow (followee_id);

CREATE TABLE handwriting_profile (
    id            BIGSERIAL PRIMARY KEY,
    owner_id      BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          VARCHAR(80)  NOT NULL,
    covered_chars TEXT         NOT NULL,
    missing_chars TEXT         NOT NULL DEFAULT '',
    glyph_pack    BYTEA        NOT NULL,
    consent_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_hw_owner ON handwriting_profile (owner_id);
