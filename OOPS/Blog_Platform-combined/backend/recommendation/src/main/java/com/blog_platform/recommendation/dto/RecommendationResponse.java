package com.blog_platform.recommendation.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecommendationResponse {
    private List<Long> blogIds;
    private String recommendationType;

    public RecommendationResponse(List<Long> blogIds, String topicBased) {
        this.blogIds = blogIds;
        this.recommendationType = topicBased;
    }
}