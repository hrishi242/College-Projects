package com.blog_platform.recommendation.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmbeddingService {

    private static final int TITLE_EMBEDDING_DIM = 384;
    private static final int CONTENT_EMBEDDING_DIM = 1024;
    private static final int MAX_CONTENT_LENGTH = 5000;

    private final RestTemplate restTemplate;
    private final String embeddingServiceUrl;
    private final ObjectMapper objectMapper;
    private boolean serviceAvailable = false;

    public EmbeddingService(
            @Value("${embedding.service.url:http://embedding-service:8000}") String embeddingServiceUrl,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.embeddingServiceUrl = embeddingServiceUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        checkServiceAvailability();
    }

    private void checkServiceAvailability() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(embeddingServiceUrl + "/health", Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                if (body != null && "OK".equals(body.get("status")) && Boolean.TRUE.equals(body.get("models_loaded"))) {
                    serviceAvailable = true;
                    log.info("Embedding service is available and models are loaded");
                } else {
                    log.warn("Embedding service is available but models are not loaded");
                }
            } else {
                log.warn("Embedding service health check failed: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to connect to embedding service", e);
        }
    }

    public boolean isModelsLoaded() {
        return serviceAvailable;
    }

    public float[] generateTitleEmbedding(String title) {
        if (!serviceAvailable || title == null || title.trim().isEmpty()) {
            log.warn("Title embedding requested but service not available - returning zero vector");
            return new float[TITLE_EMBEDDING_DIM];
        }
        try {
            EmbeddingRequest request = new EmbeddingRequest(title);
            EmbeddingResponse response = callEmbeddingService("/embed/title", request);
            if (response != null && response.isSuccess()) {
                return convertListToArray(response.getEmbedding());
            } else {
                log.error("Failed to generate title embedding: {}",
                        response != null ? response.getMessage() : "null response");
                return new float[TITLE_EMBEDDING_DIM];
            }
        } catch (Exception e) {
            log.error("Error generating title embedding", e);
            return new float[TITLE_EMBEDDING_DIM];
        }
    }

    public float[] generateContentEmbedding(String content) {
        if (!serviceAvailable || content == null || content.trim().isEmpty()) {
            log.warn("Content embedding requested but service not available - returning zero vector");
            return new float[CONTENT_EMBEDDING_DIM];
        }
        try {
            String truncated = content.length() > MAX_CONTENT_LENGTH ?
                    content.substring(0, MAX_CONTENT_LENGTH) : content;
            EmbeddingRequest request = new EmbeddingRequest(truncated);
            EmbeddingResponse response = callEmbeddingService("/embed/content", request);
            if (response != null && response.isSuccess()) {
                return convertListToArray(response.getEmbedding());
            } else {
                log.error("Failed to generate content embedding: {}",
                        response != null ? response.getMessage() : "null response");
                return new float[CONTENT_EMBEDDING_DIM];
            }
        } catch (Exception e) {
            log.error("Error generating content embedding", e);
            return new float[CONTENT_EMBEDDING_DIM];
        }
    }

    private EmbeddingResponse callEmbeddingService(String endpoint, EmbeddingRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<EmbeddingResponse> response =
                    restTemplate.postForEntity(embeddingServiceUrl + endpoint, entity, EmbeddingResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to call embedding service at {}", endpoint, e);
            return null;
        }
    }

    private float[] convertListToArray(List<Float> list) {
        if (list == null) {
            return new float[0];
        }
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public String arrayToPgVector(float[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public String generateTitleEmbeddingAsPgVector(String title) {
        return arrayToPgVector(generateTitleEmbedding(title));
    }

    public String generateContentEmbeddingAsPgVector(String content) {
        return arrayToPgVector(generateContentEmbedding(content));
    }

    public boolean isTitleModelAvailable() {
        return serviceAvailable;
    }

    public boolean isContentModelAvailable() {
        return serviceAvailable;
    }

    @Data
    private static class EmbeddingRequest {
        private final String text;
    }

    @Data
    private static class EmbeddingResponse {
        private List<Float> embedding;
        private String model;

        @JsonProperty("success")
        private boolean success;

        private String message = "";
    }
}