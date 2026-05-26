package com.boot.blog.service;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Page;
import com.boot.blog.model.Post;
import com.boot.blog.repository.PostRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PostService {

    @Value("${app.upload.dir:./uploads/}")
    private String uploadDir;

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public Page getPage(String search, int pageNumber, int pageSize) {
        List<Post> posts = postRepository.findAll(search, pageNumber, pageSize);
        int total = postRepository.countAll(search);
        int lastPage = Math.max(1, (int) Math.ceil((double) total / pageSize));
        return new Page(posts, pageNumber > 1, pageNumber < lastPage, lastPage);
    }

    public Post findById(long id) {
        return postRepository.findById(id);
    }

    public Post savePost(NewPostDto postDto) {
        return postRepository.save(postDto);
    }

    public void updateImage(long postId, MultipartFile image) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Path filePath = Paths.get(uploadDir, postId + ".jpeg");
        Files.write(filePath, image.getBytes());
    }

    public Resource download(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            byte[] content = Files.readAllBytes(filePath);
            return new ByteArrayResource(content);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<CommentDto> findAllCommentsForPost(long id) {
        return postRepository.findAllCommentsForPost(id);
    }

    public CommentDto saveComment(CommentDto commentDto) {
        return postRepository.saveComment(commentDto);
    }

    public void deleteComment(long postId, long commentId) {
        postRepository.deleteComment(postId, commentId);
    }

    public CommentDto updateComment(long postId, long commentId, String text) {
        return postRepository.updateComment(postId, commentId, text);
    }

    public Post updatePost(UpdatePostDto updatePostDto) {
        return postRepository.updatePost(updatePostDto);
    }

    public void deletePost(long postId) {
        postRepository.deletePost(postId);
    }

    public Post incrementLikes(long id) {
        return postRepository.incrementLikes(id);
    }
}
