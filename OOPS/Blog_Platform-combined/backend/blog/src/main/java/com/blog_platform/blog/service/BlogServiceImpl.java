package com.blog_platform.blog.service;

import com.blog_platform.blog.dto.BlogRequest;
import com.blog_platform.blog.dto.BlogResponse;
import com.blog_platform.blog.dto.BlogEvent;
import com.blog_platform.blog.exceptions.BlogNotFoundException;
import com.blog_platform.blog.model.Blog;
import com.blog_platform.blog.model.Comment;
import com.blog_platform.blog.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public BlogResponse createBlog(BlogRequest blogRequest, String userId) {
        Blog blog = new Blog();
        blog.setTopics(blogRequest.getTopics());
        blog.setTitle(blogRequest.getTitle());
        blog.setContent(blogRequest.getContent());
        blog.setPreviewImage(blogRequest.getPreviewImage());
        blog.setAnonymous(blogRequest.isAnonymous());
        blog.setUserId(userId);

        Blog savedBlog = blogRepository.save(blog);

        // Send Kafka event
        kafkaProducerService.sendBlogEvent(
                "CREATE",
                savedBlog.getId(),
                savedBlog.getTitle(),
                savedBlog.getContent(),
                savedBlog.getTopics().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()),
                userId
        );

        return mapToBlogResponse(savedBlog, userId);
    }

    @Override
    public BlogResponse getBlogById(Long id) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));
        return mapToBlogResponse(blog, null);
    }

    @Override
    public List<BlogResponse> getAllBlogs() {
        return blogRepository.findAll().stream()
                .map(blog -> mapToBlogResponse(blog, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> getBlogsByUser(String userId) {
        return blogRepository.findByUserId(userId).stream()
                .map(blog -> mapToBlogResponse(blog, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BlogResponse updateBlog(Long id, BlogRequest blogRequest, String userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));

        if (!blog.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this blog");
        }

        blog.setTitle(blogRequest.getTitle());
        blog.setTopics(blogRequest.getTopics());
        blog.setContent(blogRequest.getContent());
        blog.setPreviewImage(blogRequest.getPreviewImage());
        blog.setAnonymous(blogRequest.isAnonymous());

        Blog updatedBlog = blogRepository.save(blog);

        // Send Kafka event
        kafkaProducerService.sendBlogEvent(
                "UPDATE",
                updatedBlog.getId(),
                updatedBlog.getTitle(),
                updatedBlog.getContent(),
                updatedBlog.getTopics().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()),
                userId
        );

        return mapToBlogResponse(updatedBlog, userId);
    }

    @Override
    @Transactional
    public void deleteBlog(Long id, String userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));

        if (!blog.getUserId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this blog");
        }

        // Send Kafka event before deletion
        kafkaProducerService.sendBlogEvent(
                "DELETE",
                blog.getId(),
                blog.getTitle(),
                blog.getContent(),
                blog.getTopics().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()),
                userId
        );

        blogRepository.delete(blog);
    }

    @Override
    @Transactional
    public BlogResponse likeBlog(Long id, String userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));

        if (blog.getLikedByUsers().contains(userId)) {
            blog.getLikedByUsers().remove(userId);
        } else if (blog.getDislikedByUsers().contains(userId)) {
            blog.getDislikedByUsers().remove(userId);
            blog.getLikedByUsers().add(userId);
        } else {
            blog.getLikedByUsers().add(userId);
        }

        Blog updatedBlog = blogRepository.save(blog);
        return mapToBlogResponse(updatedBlog, userId);
    }

    @Override
    @Transactional
    public BlogResponse dislikeBlog(Long id, String userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));

        if (blog.getDislikedByUsers().contains(userId)) {
            blog.getDislikedByUsers().remove(userId);
        } else if (blog.getLikedByUsers().contains(userId)) {
            blog.getLikedByUsers().remove(userId);
            blog.getDislikedByUsers().add(userId);
        } else {
            blog.getDislikedByUsers().add(userId);
        }

        Blog updatedBlog = blogRepository.save(blog);
        return mapToBlogResponse(updatedBlog, userId);
    }

    @Override
    @Transactional
    public BlogResponse addComment(Long id, String commentContent, String userId) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new BlogNotFoundException("Blog not found with id: " + id));

        Comment comment = new Comment();
        comment.setContent(commentContent);
        comment.setUserId(userId);
        comment.setBlog(blog);

        blog.getComments().add(comment);

        Blog updatedBlog = blogRepository.save(blog);
        return mapToBlogResponse(updatedBlog, userId);
    }

    // Update the mapToBlogResponse method:
    private BlogResponse mapToBlogResponse(Blog blog, String currentUserId) {
        BlogResponse response = new BlogResponse();
        response.setId(blog.getId());
        response.setTopics(blog.getTopics());
        response.setTitle(blog.getTitle());
        response.setContent(blog.getContent());
        response.setPreviewImage(blog.getPreviewImage());
        response.setCreatedAt(blog.getCreatedAt());
        response.setUpdatedAt(blog.getUpdatedAt());
        if (currentUserId != null) {
            response.setLikedByCurrentUser(blog.getLikedByUsers().contains(currentUserId));
            response.setDislikedByCurrentUser(blog.getDislikedByUsers().contains(currentUserId));
        } else {
            response.setLikedByCurrentUser(false);
            response.setDislikedByCurrentUser(false);
        }
        response.setLikes(blog.getLikedByUsers().size());
        response.setDislikes(blog.getDislikedByUsers().size());
        response.setLikedByCurrentUser(currentUserId != null && blog.getLikedByUsers().contains(currentUserId));
        response.setDislikedByCurrentUser(currentUserId != null && blog.getDislikedByUsers().contains(currentUserId));
        response.setComments(blog.getComments());
        response.setAnonymous(blog.isAnonymous());
        response.setUserId(blog.isAnonymous() ? null : blog.getUserId());
        return response;
    }
}