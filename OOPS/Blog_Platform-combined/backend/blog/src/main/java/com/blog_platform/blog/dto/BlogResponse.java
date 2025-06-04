package com.blog_platform.blog.dto;

import com.blog_platform.blog.enums.Topic;
import com.blog_platform.blog.model.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogResponse {
    private Long id;
    private String title;
    private String content;
    private String previewImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int likes;
    private int dislikes;
    private boolean isLikedByCurrentUser;
    private boolean isDislikedByCurrentUser;
    private List<Comment> comments;
    private boolean anonymous;
    private String userId;
    private List<Topic> topics;  // Updated to be a list of Topic enums
}