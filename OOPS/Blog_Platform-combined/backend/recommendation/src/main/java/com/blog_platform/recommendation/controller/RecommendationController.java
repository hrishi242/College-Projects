package com.blog_platform.recommendation.controller;


import com.blog_platform.recommendation.dto.RecommendationResponse;
import com.blog_platform.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/topic-based")
    public RecommendationResponse getTopicBasedRecommendations(
            @RequestParam List<String> topics,
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> blogIds = recommendationService.getTopicBasedRecommendations(topics, limit);
        return new RecommendationResponse(blogIds, "TOPIC_BASED");
    }

    @GetMapping("/hybrid")
    public RecommendationResponse getHybridRecommendations(
            @RequestParam List<String> topics,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> blogIds = recommendationService.getHybridRecommendations(topics, query, limit);
        return new RecommendationResponse(blogIds, "HYBRID");
    }
    @GetMapping("/similar-blogs")
    public RecommendationResponse getSimilarRecommendations(
            @RequestParam List<String> topics,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> blogIds = recommendationService.getSimilarRecommendations(topics, title, content, limit);
        return new RecommendationResponse(blogIds, "SIMILAR_BLOGS");
    }
    @GetMapping("/content-based")
    public RecommendationResponse getContentBasedRecommendations(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        List<Long> blogIds = recommendationService.getContentBasedRecommendations(query, limit);
        return new RecommendationResponse(blogIds, "CONTENT_BASED");
    }
}
