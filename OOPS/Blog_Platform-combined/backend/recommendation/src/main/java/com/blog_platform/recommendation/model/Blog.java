package com.blog_platform.recommendation.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Entity
@Table(name = "blogs")
public class Blog {
    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Version
//    private Long version;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id")
    private String userId;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "blog_topics",
            joinColumns = @JoinColumn(name = "blog_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private List<Topic> topics;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "title_embedding", columnDefinition = "vector(384)")
    private float[] titleEmbedding;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "content_embedding", columnDefinition = "vector(1024)")
    private float[] contentEmbedding;

    public void setTopicNames(List<String> topicNames) {
        this.topics = topicNames.stream()
                .map(name -> {
                    Topic topic = new Topic();
                    topic.setName(name);
                    return topic;
                })
                .collect(Collectors.toList());
    }
}