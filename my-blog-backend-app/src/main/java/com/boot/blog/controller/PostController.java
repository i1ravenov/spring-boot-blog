package com.boot.blog.controller;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Page;
import com.boot.blog.model.Post;
import com.boot.blog.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public Page getPage(@RequestParam("search") String search,
                        @RequestParam("pageNumber") int pageNumber,
                        @RequestParam("pageSize") int pageSize) {
        return new Page(postService.findAll(search), false, false, 1);
    }

    @PostMapping("/posts")
    public Post createNewPost(@RequestBody NewPostDto newPostDto) {
        return postService.savePost(newPostDto);
    }

    @GetMapping("/posts/{id}")
    public Post getPost(@PathVariable long id) {
        return postService.findById(id);
    }

    @GetMapping("/posts/{id}/comments")
    public List<CommentDto> getCommentsForPost(@PathVariable String id) {
        try {
            return postService.findAllCommentsForPost(Long.parseLong(id));
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    @GetMapping("/posts/{id}/image")
    public ResponseEntity<Resource> getPostImage(@PathVariable long id) throws IOException {
        Path imagePath = Paths.get(postService.getUploadDir() + id + ".jpeg");

        Resource resource;
        if (Files.exists(imagePath)) {
            resource = new UrlResource(imagePath.toUri());
        } else {
            resource = new ClassPathResource("static/default.jpeg");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(resource);
    }

    @PutMapping("/posts/{id}/image")
    public ResponseEntity<Void> updatePostImage(
            @PathVariable long id,
            @RequestParam("image") MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        postService.updateImage(id, image);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts/{id}/comments")
    public CommentDto addComment(@PathVariable long id, @RequestBody CommentDto commentDto) {
        return postService.saveComment(new CommentDto(0, commentDto.text(), id));
    }

    @DeleteMapping("/posts/{postId}/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long postId, @PathVariable long id) {
        postService.deleteComment(postId, id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/posts/{postId}/comments/{id}")
    public CommentDto updateComment(
            @PathVariable long postId,
            @PathVariable long id,
            @RequestBody CommentDto commentDto) {
        return postService.updateComment(postId, id, commentDto.text());
    }

    @PutMapping("/posts/{id}")
    public Post updatePost(@RequestBody UpdatePostDto updatePostDto) {
        return postService.updatePost(updatePostDto);
    }

    @DeleteMapping("/posts/{id}")
    public void deletePost(@PathVariable long id) {
        postService.deletePost(id);
    }

    @PostMapping("/posts/{id}/likes")
    public int likePost(@PathVariable long id) {
        return postService.incrementLikes(id).getLikesCount();
    }
}
