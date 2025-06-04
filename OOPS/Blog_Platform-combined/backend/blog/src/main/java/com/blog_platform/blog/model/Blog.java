package com.blog_platform.blog.model;

import com.blog_platform.blog.enums.Topic;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Remove the single topic field
    // @Column(nullable = false)
    // private String topic;

    // Add the list of topics using ElementCollection
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "blog_topics", joinColumns = @JoinColumn(name = "blog_id"))
    @Column(name = "topic")
    private List<Topic> topics = new ArrayList<>();

    @Column(name = "preview_image")
    private String previewImage;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "blog_likes", joinColumns = @JoinColumn(name = "blog_id"))
    private Set<String> likedByUsers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "blog_dislikes", joinColumns = @JoinColumn(name = "blog_id"))
    private Set<String> dislikedByUsers = new HashSet<>();

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(nullable = false)
    private boolean anonymous = false;

    @Column(name = "user_id", nullable = false)
    private String userId; // Stores the ID of the user who created the blog
}