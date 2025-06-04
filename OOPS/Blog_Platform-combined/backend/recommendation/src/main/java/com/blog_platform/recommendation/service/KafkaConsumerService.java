package com.blog_platform.recommendation.service;

import com.blog_platform.recommendation.dto.BlogEvent;
import com.blog_platform.recommendation.model.Blog;
import com.blog_platform.recommendation.model.Topic;
import com.blog_platform.recommendation.repository.BlogRepository;
import com.blog_platform.recommendation.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    private final BlogRepository blogRepository;
    private final TopicRepository topicRepository;
    private final EmbeddingService embeddingService;

    @Value("${embedding.service.enabled:true}")
    private boolean embeddingServiceEnabled;

    @KafkaListener(topics = "blog-events", groupId = "recommendation-group")
    public void consumeBlogEvent(BlogEvent blogEvent) {
        log.info("Received blog event: {}", blogEvent);

        try {
            processEvent(blogEvent);
        } catch (OptimisticLockingFailureException e) {
            log.error("OptimisticLockingFailureException", e);
            log.info("Skipping duplicate processing for blog ID: {} - entity was already updated", blogEvent.getBlogId());
        } catch (Exception e) {
            log.error("Error processing blog event: {}", blogEvent, e);
            throw e;
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processEvent(BlogEvent blogEvent) {
        try {
            switch (blogEvent.getEventType()) {
                case "CREATE", "UPDATE" -> handleCreateOrUpdateEvent(blogEvent);
                case "DELETE" -> handleDeleteEvent(blogEvent);
                default -> log.warn("Unknown event type: {}", blogEvent.getEventType());
            }
        } catch (Exception e) {
            log.error("Transaction failed for event {}", blogEvent, e);
            throw e;
        }
    }

    private void handleCreateOrUpdateEvent(BlogEvent blogEvent) {
        Optional<Blog> existingBlog = blogRepository.findById(blogEvent.getBlogId());
        Blog blog = existingBlog.orElse(new Blog());

        blog.setId(blogEvent.getBlogId());
        blog.setTitle(blogEvent.getTitle());
        blog.setContent(blogEvent.getContent());
        blog.setUserId(blogEvent.getUserId());

        if (blogEvent.getTopics() != null) {
            blog.setTopics(blogEvent.getTopics().stream()
                    .map(topicName -> topicRepository.findByName(topicName)
                            .orElseGet(() -> {
                                Topic newTopic = new Topic();
                                newTopic.setName(topicName);
                                return topicRepository.save(newTopic);
                            }))
                    .toList());
        }
        log.debug("Topics set for blog {}: {}", blogEvent.getBlogId(), blog.getTopics());

        // Only generate embeddings if the service is enabled and available
        if (embeddingServiceEnabled && embeddingService.isModelsLoaded()) {
            // Generate title embedding
            float[] titleEmbedding = embeddingService.generateTitleEmbedding(blogEvent.getTitle());
            blog.setTitleEmbedding(titleEmbedding);
            log.debug("Title embedding for blog {}: {}", blogEvent.getBlogId(), Arrays.toString(titleEmbedding));

            // Generate content embedding
            float[] contentEmbedding = embeddingService.generateContentEmbedding(blogEvent.getContent());
            blog.setContentEmbedding(contentEmbedding);
            log.debug("Content embedding for blog {}: {}", blogEvent.getBlogId(), Arrays.toString(contentEmbedding));
        } else {
            log.warn("Skipping embedding generation for blog {} - service not available", blogEvent.getBlogId());
        }

        Blog savedBlog = blogRepository.save(blog);
        log.info("Processed {} event for blog {}", blogEvent.getEventType(), savedBlog.getId());
    }

    private void handleDeleteEvent(BlogEvent blogEvent) {
        Optional<Blog> blogOptional = blogRepository.findById(blogEvent.getBlogId());
        if (blogOptional.isPresent()) {
            blogRepository.delete(blogOptional.get());
            log.info("Deleted blog with ID: {}", blogEvent.getBlogId());
        } else {
            log.warn("Attempted to delete non-existent blog with ID: {}", blogEvent.getBlogId());
        }
    }
}