
CREATE EXTENSION IF NOT EXISTS vector;

-- Main blog table
CREATE TABLE IF NOT EXISTS blogs (
    id BIGSERIAL PRIMARY KEY,
    title TEXT,
    content TEXT,
    user_id TEXT,
    title_embedding vector(384),  -- Using pgvector's vector type
    content_embedding vector(1024) -- Adjust dimension as needed
);

-- Topics lookup table
CREATE TABLE IF NOT EXISTS topics (
    id BIGSERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

-- Blog-Topic many-to-many relationship
CREATE TABLE IF NOT EXISTS blog_topics (
    blog_id BIGINT REFERENCES blogs(id) ON DELETE CASCADE,
    topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (blog_id, topic_id)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_blog_title_embedding ON blogs USING ivfflat (title_embedding vector_l2_ops);
CREATE INDEX IF NOT EXISTS idx_blog_content_embedding ON blogs USING ivfflat (content_embedding vector_l2_ops);
CREATE INDEX IF NOT EXISTS idx_topic_name ON topics(name);
