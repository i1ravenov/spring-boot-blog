package com.boot.blog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM comment");
        jdbcTemplate.execute("DELETE FROM post");
        jdbcTemplate.execute("""
                INSERT INTO post (id, title, text, tags, likes_count, comments_count)
                VALUES (1, 'First Post', 'This is my first post', '["java","spring"]', 10, 2)
                """);
        jdbcTemplate.execute("""
                INSERT INTO post (id, title, text, tags, likes_count, comments_count)
                VALUES (2, 'Second Post', 'Learning Spring is fun', '["spring","backend"]', 25, 5)
                """);
        jdbcTemplate.execute("""
                INSERT INTO post (id, title, text, tags, likes_count, comments_count)
                VALUES (3, 'Third Post', 'Microservices guide', '["microservices","architecture"]', 40, 8)
                """);
        jdbcTemplate.execute("INSERT INTO comment (id, text, post_id) VALUES (1, 'Комментарий к посту 1', 1)");
        jdbcTemplate.execute("INSERT INTO comment (id, text, post_id) VALUES (2, 'Ещё один комментарий к посту 1', 1)");
        jdbcTemplate.execute("ALTER TABLE post ALTER COLUMN id RESTART WITH 4");
        jdbcTemplate.execute("ALTER TABLE comment ALTER COLUMN id RESTART WITH 3");
    }

    @Test
    void getPosts_returnsPageWithAllPosts() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(3)))
                .andExpect(jsonPath("$.posts[0].title").value("First Post"))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getPosts_pagination_returnsCorrectPage() throws Exception {
        // pageSize=2: страница 1 → посты 1,2; страница 2 → пост 3
        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.lastPage").value(2));

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(2));
    }

    @Test
    void likePost_incrementsLikesCount() throws Exception {
        // @BeforeEach устанавливает likes_count = 10 для поста 1
        mockMvc.perform(post("/api/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(11));

        mockMvc.perform(post("/api/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(12));
    }

    @Test
    void likePost_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/posts/999/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPost_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Post not found: id=999"));
    }

    @Test
    void updatePost_nonExistent_returns404() throws Exception {
        String json = """
                {"id":999,"title":"X","text":"Y","tags":[]}
                """;
        mockMvc.perform(put("/api/posts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPosts_withTagSearch_returnsFilteredPosts() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "java")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].title").value("First Post"));

        mockMvc.perform(get("/api/posts")
                        .param("search", "spring")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)));

        mockMvc.perform(get("/api/posts")
                        .param("search", "nonexistent")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(0)));
    }

    @Test
    void getPost_returnsPost() throws Exception {
        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("First Post"))
                .andExpect(jsonPath("$.text").value("This is my first post"));
    }

    @Test
    void createPost_persistsAndReturns() throws Exception {
        String json = """
                {"title":"New Post","text":"New content","tags":["test"]}
                """;

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.title").value("New Post"));

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(4)));
    }

    @Test
    void updatePost_changesFields() throws Exception {
        String json = """
                {"id":1,"title":"Updated Title","text":"Updated text","tags":["updated"]}
                """;

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    void deletePost_removesFromDb() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts", hasSize(2)));
    }

    @Test
    void getComments_returnsAllForPost() throws Exception {
        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].text").value("Комментарий к посту 1"));
    }

    @Test
    void addComment_persistsAndReturns() throws Exception {
        String json = """
                {"id":0,"text":"Новый комментарий","postId":1}
                """;

        mockMvc.perform(post("/api/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Новый комментарий"))
                .andExpect(jsonPath("$.postId").value(1));

        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void deleteComment_removesFromPost() throws Exception {
        mockMvc.perform(delete("/api/posts/1/comments/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateComment_changesText() throws Exception {
        String json = """
                {"id":1,"text":"Обновлённый текст","postId":1}
                """;

        mockMvc.perform(put("/api/posts/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value("Обновлённый текст"));
    }

    @Test
    void getPostImage_returnsJpeg() throws Exception {
        mockMvc.perform(get("/api/posts/1/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }
}
