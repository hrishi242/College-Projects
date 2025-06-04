package com.blog_platform.recommendation.repository;

import com.blog_platform.recommendation.model.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    @Query(value = """
        SELECT b.id FROM blogs b
        JOIN blog_topics bt ON b.id = bt.blog_id
        JOIN topics t ON bt.topic_id = t.id
        WHERE LOWER(t.name) IN (:topics)
        GROUP BY b.id
        ORDER BY COUNT(t.id) DESC, b.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findBlogIdsByTopics(@Param("topics") List<String> topics, @Param("limit") int limit);

    @Query(value = """
        SELECT b.id FROM blogs b
        JOIN blog_topics bt ON b.id = bt.blog_id
        JOIN topics t ON bt.topic_id = t.id
        WHERE LOWER(t.name) IN (:topics)
        GROUP BY b.id
        ORDER BY (1 - (b.title_embedding <=> CAST(:titleEmbedding AS vector))) * 0.5 + 
                 (1 - (b.content_embedding <=> CAST(:contentEmbedding AS vector))) * 0.5 DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findBlogIdsByTopicsAndContent(
            @Param("topics") List<String> topics,
            @Param("titleEmbedding") String titleEmbedding,
            @Param("contentEmbedding") String contentEmbedding,
            @Param("limit") int limit);

    @Query(value = """
        SELECT b.id FROM blogs b
        ORDER BY 1 - (b.content_embedding <=> CAST(:contentEmbedding AS vector)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> findBlogIdsByContent(
            @Param("contentEmbedding") String contentEmbedding,
            @Param("limit") int limit);
}