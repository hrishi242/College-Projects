package com.blog_platform.recommendation.service;

import com.blog_platform.recommendation.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private static final Logger logger = Logger.getLogger(RecommendationService.class.getName());

    private final BlogRepository blogRepository;
    private final EmbeddingService embeddingService;

    public List<Long> getTopicBasedRecommendations(List<String> topics, int limit) {
        if (topics == null || topics.isEmpty()) {
            logger.info("No topics provided for recommendation");
            return Collections.emptyList();
        }

        // Clean topics by removing quotes and whitespace
        List<String> cleanedTopics = topics.stream()
                .map(topic -> topic.replaceAll("^\"|\"$", "").trim())
                .collect(Collectors.toList());

        logger.info("Raw topics after cleaning: " + cleanedTopics);

        // Convert to lowercase
        List<String> lowercaseTopics = cleanedTopics.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        logger.info("Getting recommendations for topics: " + lowercaseTopics);
        return blogRepository.findBlogIdsByTopics(lowercaseTopics, limit);
    }

    public List<Long> getContentBasedRecommendations(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            logger.info("Empty query provided for content-based recommendation");
            return Collections.emptyList();
        }

        if (!embeddingService.isModelsLoaded()) {
            logger.warning("Embedding models not loaded, returning empty recommendations");
            return Collections.emptyList();
        }

        String embedding = embeddingService.arrayToPgVector(
                embeddingService.generateContentEmbedding(query)
        );

        logger.info("Getting content-based recommendations for query: " + query);
        return blogRepository.findBlogIdsByContent(embedding, limit);
    }

    public List<Long> getHybridRecommendations(List<String> topics, String query, int limit) {
        if (topics == null || topics.isEmpty() || query == null || query.trim().isEmpty()) {
            logger.info("Incomplete parameters for hybrid recommendations");
            return Collections.emptyList();
        }

        // Clean and lowercase topics
        List<String> lowercaseTopics = topics.stream()
                .map(topic -> topic.replaceAll("^\"|\"$", "").trim())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        String titleEmbedding = embeddingService.arrayToPgVector(
                embeddingService.generateTitleEmbedding(query)
        );
        String contentEmbedding = embeddingService.arrayToPgVector(
                embeddingService.generateContentEmbedding(query)
        );

        logger.info("Getting hybrid recommendations for topics: " + lowercaseTopics + " and query: " + query);
        return blogRepository.findBlogIdsByTopicsAndContent(
                lowercaseTopics, titleEmbedding, contentEmbedding, limit
        );
    }

    public List<Long> getSimilarRecommendations(List<String> topics, String title, String content, int limit) {
        if (topics == null || topics.isEmpty()) {
            logger.info("No topics provided for similar recommendations");
            return Collections.emptyList();
        }

        // Clean and lowercase topics
        List<String> lowercaseTopics = topics.stream()
                .map(topic -> topic.replaceAll("^\"|\"$", "").trim())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        String titleEmbedding = embeddingService.arrayToPgVector(
                embeddingService.generateTitleEmbedding(title)
        );
        String contentEmbedding = embeddingService.arrayToPgVector(
                embeddingService.generateContentEmbedding(content)
        );

        logger.info("Getting similar recommendations for topics: " + lowercaseTopics);
        return blogRepository.findBlogIdsByTopicsAndContent(
                lowercaseTopics, titleEmbedding, contentEmbedding, limit
        );
    }
}