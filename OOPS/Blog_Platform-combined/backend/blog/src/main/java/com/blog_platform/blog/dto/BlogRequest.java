package com.blog_platform.blog.dto;

import com.blog_platform.blog.enums.Topic;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BlogRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotEmpty(message = "At least one topic must be selected")
    private List<Topic> topics;

    private String previewImage;
    private boolean anonymous;
}