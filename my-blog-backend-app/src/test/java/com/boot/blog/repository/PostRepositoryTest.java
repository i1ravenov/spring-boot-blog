package com.boot.blog.repository;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.exception.PostNotFoundException;
import com.boot.blog.model.Post;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(PostRepositoryTest.TestConfig.class)
class PostRepositoryTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        JdbcNativePostRepository jdbcNativePostRepository(JdbcTemplate jdbcTemplate) {
            return new JdbcNativePostRepository(jdbcTemplate);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JdbcNativePostRepository repository;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM comment");
        jdbcTemplate.execute("DELETE FROM post_tag");
        jdbcTemplate.execute("DELETE FROM post");
        jdbcTemplate.execute("DELETE FROM tag");
        jdbcTemplate.execute("""
                INSERT INTO post (id, title, text, likes_count, comments_count)
                VALUES (1, 'Test Post', 'Test content', 5, 1)
                """);
        jdbcTemplate.execute("INSERT INTO tag (id, name) VALUES (1, 'java')");
        jdbcTemplate.execute("INSERT INTO post_tag (post_id, tag_id) VALUES (1, 1)");
        jdbcTemplate.execute("INSERT INTO comment (id, text, post_id) VALUES (1, 'Test comment', 1)");
        jdbcTemplate.execute("ALTER TABLE post    ALTER COLUMN id RESTART WITH 2");
        jdbcTemplate.execute("ALTER TABLE comment ALTER COLUMN id RESTART WITH 2");
        jdbcTemplate.execute("ALTER TABLE tag     ALTER COLUMN id RESTART WITH 2");
    }

    @Test
    void findAll_withEmptySearch_returnsAllPosts() {
        List<Post> posts = repository.findAll("", 1, 10);
        assertThat(posts).hasSize(1);
        assertThat(posts.get(0).getTitle()).isEqualTo("Test Post");
        assertThat(posts.get(0).getTags()).containsExactly("java");
    }

    @Test
    void findAll_pagination_limitsResults() {
        jdbcTemplate.execute("INSERT INTO post (id, title, text) VALUES (2, 'Post 2', 'Content')");
        jdbcTemplate.execute("INSERT INTO post (id, title, text) VALUES (3, 'Post 3', 'Content')");

        List<Post> page1 = repository.findAll("", 1, 2);
        List<Post> page2 = repository.findAll("", 2, 2);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
        assertThat(page1.get(0).getId()).isEqualTo(1);
        assertThat(page2.get(0).getId()).isEqualTo(3);
    }

    @Test
    void countAll_withEmptySearch_returnsTotal() {
        assertThat(repository.countAll("")).isEqualTo(1);
    }

    @Test
    void countAll_withMatchingTag_returnsFilteredCount() {
        jdbcTemplate.execute("INSERT INTO post (id, title, text) VALUES (2, 'Post 2', 'Content')");
        jdbcTemplate.execute("INSERT INTO tag (id, name) VALUES (2, 'spring')");
        jdbcTemplate.execute("INSERT INTO post_tag (post_id, tag_id) VALUES (2, 2)");

        assertThat(repository.countAll("java")).isEqualTo(1);
        assertThat(repository.countAll("spring")).isEqualTo(1);
        assertThat(repository.countAll("")).isEqualTo(2);
    }

    @Test
    void findAll_withMatchingTag_returnsFilteredPosts() {
        jdbcTemplate.execute("INSERT INTO post (id, title, text, likes_count, comments_count) VALUES (2, 'Spring Post', 'Content', 0, 0)");
        jdbcTemplate.execute("INSERT INTO tag (id, name) VALUES (2, 'spring')");
        jdbcTemplate.execute("INSERT INTO post_tag (post_id, tag_id) VALUES (2, 2)");

        List<Post> javaResults = repository.findAll("java", 1, 10);
        assertThat(javaResults).hasSize(1);
        assertThat(javaResults.get(0).getTitle()).isEqualTo("Test Post");

        List<Post> springResults = repository.findAll("spring", 1, 10);
        assertThat(springResults).hasSize(1);
        assertThat(springResults.get(0).getTitle()).isEqualTo("Spring Post");
    }

    @Test
    void findAll_withNonMatchingTag_returnsEmpty() {
        List<Post> results = repository.findAll("nonexistent", 1, 10);
        assertThat(results).isEmpty();
    }

    @Test
    void findAll_searchIsCaseInsensitive() {
        List<Post> results = repository.findAll("JAVA", 1, 10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTags()).containsExactly("java");
    }

    @Test
    void incrementLikes_increasesCountByOne() {
        Post before = repository.findById(1);
        Post after = repository.incrementLikes(1);
        assertThat(after.getLikesCount()).isEqualTo(before.getLikesCount() + 1);
    }

    @Test
    void incrementLikes_nonExistent_throwsPostNotFoundException() {
        assertThatThrownBy(() -> repository.incrementLikes(999))
                .isInstanceOf(PostNotFoundException.class);
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
        assertThat(repository.findAll("", 1, 10)).hasSize(2);
    }

    @Test
    void updatePost_changesFields() {
        Post updated = repository.updatePost(new UpdatePostDto(1, "Updated Title", "Updated text", List.of("updated")));

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getText()).isEqualTo("Updated text");
        assertThat(updated.getTags()).containsExactly("updated");
    }

    @Test
    void findById_nonExistent_throwsPostNotFoundException() {
        assertThatThrownBy(() -> repository.findById(999))
                .isInstanceOf(PostNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void deletePost_removesFromDb() {
        repository.deletePost(1);
        assertThat(repository.findAll("", 1, 10)).isEmpty();
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
