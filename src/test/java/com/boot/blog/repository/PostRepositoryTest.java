package com.boot.blog.repository;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(PostRepositoryTest.TestConfig.class)
class PostRepositoryTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        JdbcNativePostRepository jdbcNativePostRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            return new JdbcNativePostRepository(jdbcTemplate, objectMapper);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcNativePostRepository repository;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM comment");
        jdbcTemplate.execute("DELETE FROM post");
        jdbcTemplate.execute("""
                INSERT INTO post (id, title, text, tags, likes_count, comments_count)
                VALUES (1, 'Test Post', 'Test content', '["java"]', 5, 1)
                """);
        jdbcTemplate.execute("INSERT INTO comment (id, text, post_id) VALUES (1, 'Test comment', 1)");
        jdbcTemplate.execute("ALTER TABLE post ALTER COLUMN id RESTART WITH 2");
        jdbcTemplate.execute("ALTER TABLE comment ALTER COLUMN id RESTART WITH 2");
    }

    @Test
    void findAll_returnsAllPosts() {
        List<Post> posts = repository.findAll();
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getTitle()).isEqualTo("Test Post");
        assertThat(posts.get(0).getTags()).containsExactly("java");
    }

    @Test
    void findById_returnsCorrectPost() {
        Post post = repository.findById(1);
        assertThat(post.getId()).isEqualTo(1);
        assertThat(post.getTitle()).isEqualTo("Test Post");
        assertThat(post.getText()).isEqualTo("Test content");
        assertThat(post.getLikesCount()).isEqualTo(5);
    }

    @Test
    void save_persistsAndReturnsWithGeneratedId() {
        Post saved = repository.save(new NewPostDto("New Post", "New content", List.of("spring")));

        assertThat(saved.getId()).isEqualTo(2);
        assertThat(saved.getTitle()).isEqualTo("New Post");
        assertThat(saved.getText()).isEqualTo("New content");
        assertThat(saved.getTags()).containsExactly("spring");
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void updatePost_changesFields() {
        Post updated = repository.updatePost(new UpdatePostDto(1, "Updated Title", "Updated text", List.of("updated")));

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getText()).isEqualTo("Updated text");
        assertThat(updated.getTags()).containsExactly("updated");
    }

    @Test
    void deletePost_removesFromDb() {
        repository.deletePost(1);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAllCommentsForPost_returnsComments() {
        List<CommentDto> comments = repository.findAllCommentsForPost(1);
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).text()).isEqualTo("Test comment");
        assertThat(comments.get(0).postId()).isEqualTo(1);
    }

    @Test
    void saveComment_persistsAndReturns() {
        CommentDto saved = repository.saveComment(new CommentDto(0, "New comment", 1));

        assertThat(saved.id()).isEqualTo(2);
        assertThat(saved.text()).isEqualTo("New comment");
        assertThat(saved.postId()).isEqualTo(1);
        assertThat(repository.findAllCommentsForPost(1)).hasSize(2);
    }

    @Test
    void deleteComment_removesFromPost() {
        repository.deleteComment(1, 1);
        assertThat(repository.findAllCommentsForPost(1)).isEmpty();
    }

    @Test
    void updateComment_changesText() {
        CommentDto updated = repository.updateComment(1, 1, "Updated text");

        assertThat(updated.id()).isEqualTo(1);
        assertThat(updated.text()).isEqualTo("Updated text");
        assertThat(updated.postId()).isEqualTo(1);
    }
}
