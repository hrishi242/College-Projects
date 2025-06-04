package com.blog_platform.blog.service;



import com.blog_platform.blog.dto.BlogRequest;
import com.blog_platform.blog.dto.BlogResponse;
import com.blog_platform.blog.model.Blog;

import java.util.List;

public interface BlogService {
    BlogResponse createBlog(BlogRequest blogRequest, String userId);
    BlogResponse getBlogById(Long id);
    List<BlogResponse> getAllBlogs();
    List<BlogResponse> getBlogsByUser(String userId);
    BlogResponse updateBlog(Long id, BlogRequest blogRequest, String userId);
    void deleteBlog(Long id, String userId);
    BlogResponse likeBlog(Long id, String userId);
    BlogResponse dislikeBlog(Long id, String userId);
    BlogResponse addComment(Long id, String userId, String comment);
}
