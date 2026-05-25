package com.boot.blog.service;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Post;
import com.boot.blog.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void findAll_withEmptySearch_returnsAll() {
        Post post = new Post(1, "Title", "Text", List.of("tag"), 0, 0);
        when(postRepository.findAll("")).thenReturn(List.of(post));

        List<Post> result = postService.findAll("");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Title");
        verify(postRepository).findAll("");
    }

    @Test
    void findAll_withSearch_delegatesSearchToRepository() {
        Post post = new Post(1, "Title", "Text", List.of("java"), 0, 0);
        when(postRepository.findAll("java")).thenReturn(List.of(post));

        List<Post> result = postService.findAll("java");

        assertThat(result).hasSize(1);
        verify(postRepository).findAll("java");
    }

    @Test
    void findById_delegatesToRepository() {
        Post post = new Post(1, "Title", "Text", List.of("tag"), 0, 0);
        when(postRepository.findById(1)).thenReturn(post);

        Post result = postService.findById(1);

        assertThat(result.getId()).isEqualTo(1);
        verify(postRepository).findById(1);
    }

    @Test
    void savePost_delegatesToRepository() {
        NewPostDto dto = new NewPostDto("New", "Content", List.of("java"));
        Post saved = new Post(5, "New", "Content", List.of("java"), 0, 0);
        when(postRepository.save(dto)).thenReturn(saved);

        Post result = postService.savePost(dto);

        assertThat(result.getId()).isEqualTo(5);
        assertThat(result.getTitle()).isEqualTo("New");
        verify(postRepository).save(dto);
    }

    @Test
    void deletePost_delegatesToRepository() {
        postService.deletePost(1);
        verify(postRepository).deletePost(1);
    }

    @Test
    void saveComment_delegatesToRepository() {
        CommentDto dto = new CommentDto(0, "text", 1);
        CommentDto saved = new CommentDto(10, "text", 1);
        when(postRepository.saveComment(dto)).thenReturn(saved);

        CommentDto result = postService.saveComment(dto);

        assertThat(result.id()).isEqualTo(10);
        verify(postRepository).saveComment(dto);
    }

    @Test
    void updatePost_delegatesToRepository() {
        UpdatePostDto dto = new UpdatePostDto(1, "Updated", "Text", List.of("tag"));
        Post updated = new Post(1, "Updated", "Text", List.of("tag"), 0, 0);
        when(postRepository.updatePost(dto)).thenReturn(updated);

        Post result = postService.updatePost(dto);

        assertThat(result.getTitle()).isEqualTo("Updated");
        verify(postRepository).updatePost(dto);
    }

    @Test
    void deleteComment_delegatesToRepository() {
        postService.deleteComment(1, 2);
        verify(postRepository).deleteComment(1, 2);
    }

    @Test
    void incrementLikes_delegatesToRepository() {
        Post updated = new Post(1, "Title", "Text", List.of("tag"), 6, 0);
        when(postRepository.incrementLikes(1)).thenReturn(updated);

        Post result = postService.incrementLikes(1);

        assertThat(result.getLikesCount()).isEqualTo(6);
        verify(postRepository).incrementLikes(1);
    }
}
