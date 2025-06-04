package com.blog_platform.blog.controller;


import com.blog_platform.blog.dto.BlogRequest;
import com.blog_platform.blog.dto.BlogResponse;
import com.blog_platform.blog.service.BlogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @PostMapping
    public ResponseEntity<BlogResponse> createBlog(
            @Valid @RequestBody BlogRequest blogRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("Creating blog with request: " + blogRequest);
        System.out.println("User details: " + userDetails);
        BlogResponse response = blogService.createBlog(blogRequest, userDetails.getUsername());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogResponse> getBlogById(@PathVariable Long id) {
        System.out.println("Fetching blog with ID: " + id);
        BlogResponse response = blogService.getBlogById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BlogResponse>> getAllBlogs() {
        List<BlogResponse> responses = blogService.getAllBlogs();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/user")
    public ResponseEntity<List<BlogResponse>> getBlogsByUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<BlogResponse> responses = blogService.getBlogsByUser(userDetails.getUsername());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BlogResponse> updateBlog(
            @PathVariable Long id,
            @Valid @RequestBody BlogRequest blogRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        BlogResponse response = blogService.updateBlog(id, blogRequest, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        blogService.deleteBlog(id, userDetails.getUsername());
        return ResponseEntity.ok("Deleted successfully");
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            BlogResponse response = blogService.likeBlog(id, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> dislikeBlog(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            BlogResponse response = blogService.dislikeBlog(id, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<String> addComment(
            @PathVariable Long id,
            @RequestBody String commentContent,
            @AuthenticationPrincipal UserDetails userDetails) {
        BlogResponse response = blogService.addComment(id, commentContent, userDetails.getUsername());
        return ResponseEntity.ok("Comment successfully added");
    }
}


