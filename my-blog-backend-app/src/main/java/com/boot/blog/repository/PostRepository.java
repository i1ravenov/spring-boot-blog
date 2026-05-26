package com.boot.blog.repository;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Post;

import java.util.List;

public interface PostRepository {
    List<Post> findAll(String search, int pageNumber, int pageSize);

    int countAll(String search);

    Post findById(long id);

    Post save(NewPostDto newPostDto);

    List<CommentDto> findAllCommentsForPost(long id);

    CommentDto saveComment(CommentDto commentDto);

    void deleteComment(long postId, long commentId);

    CommentDto updateComment(long postId, long commentId, String text);

    Post updatePost(UpdatePostDto updatePostDto);

    void deletePost(long postId);

    Post incrementLikes(long id);
}
