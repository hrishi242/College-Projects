package com.blog_platform.blog.dto;

import com.blog_platform.blog.enums.Topic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogEvent {
    private String eventType;
    private Long blogId;
    private String title;
    private String content;
    private List<String> topics;
    private String userId;
}
