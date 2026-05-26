package com.boot.blog.service;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Page;
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
    void getPage_returnsPostsWithCorrectPaginationMetadata() {
        Post post = new Post(1, "Title", "Text", List.of("tag"), 0, 0);
        when(postRepository.findAll("", 1, 2)).thenReturn(List.of(post, post));
        when(postRepository.countAll("")).thenReturn(5);

        Page page = postService.getPage("", 1, 2);

        assertThat(page.getPosts()).hasSize(2);
        assertThat(page.isHasPrev()).isFalse();
        assertThat(page.isHasNext()).isTrue();
        assertThat(page.getLastPage()).isEqualTo(3); // ceil(5/2) = 3
    }

    @Test
    void getPage_lastPage_hasNoNext() {
        Post post = new Post(1, "Title", "Text", List.of("tag"), 0, 0);
        when(postRepository.findAll("", 3, 2)).thenReturn(List.of(post));
        when(postRepository.countAll("")).thenReturn(5);

        Page page = postService.getPage("", 3, 2);

        assertThat(page.isHasPrev()).isTrue();
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getLastPage()).isEqualTo(3);
    }

    @Test
    void getPage_emptyResult_lastPageIsOne() {
        when(postRepository.findAll("nothing", 1, 10)).thenReturn(List.of());
        when(postRepository.countAll("nothing")).thenReturn(0);

        Page page = postService.getPage("nothing", 1, 10);

        assertThat(page.getPosts()).isEmpty();
        assertThat(page.isHasPrev()).isFalse();
        assertThat(page.isHasNext()).isFalse();
        assertThat(page.getLastPage()).isEqualTo(1);
    }

    @Test
    void getPage_withTagSearch_delegatesSearchToRepository() {
        when(postRepository.findAll("java", 1, 10)).thenReturn(List.of());
        when(postRepository.countAll("java")).thenReturn(0);

        postService.getPage("java", 1, 10);

        verify(postRepository).findAll("java", 1, 10);
        verify(postRepository).countAll("java");
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
