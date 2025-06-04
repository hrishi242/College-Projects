package com.blog_platform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("blogEvent")
public class BlogEvent {
    private String eventType; // CREATE, UPDATE, DELETE
    private Long blogId;
    private String title;
    private String content;
    private List<String> topics;
    private String userId;
}