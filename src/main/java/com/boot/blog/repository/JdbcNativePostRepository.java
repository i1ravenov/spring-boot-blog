package com.boot.blog.repository;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.model.Post;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcNativePostRepository implements PostRepository {

    private final ObjectMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public JdbcNativePostRepository(JdbcTemplate jdbcTemplate, ObjectMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public List<Post> findAll() {
        return jdbcTemplate.query(
                "SELECT id, title, text, tags, likes_count, comments_count FROM post",
                (rs, rowNum) -> {
                    try {
                        return new Post(
                                rs.getLong("id"),
                                rs.getString("title"),
                                rs.getString("text"),
                                mapper.readValue(rs.getString("tags"),
                                        mapper.getTypeFactory().constructCollectionType(List.class, String.class)),
                                rs.getInt("likes_count"),
                                rs.getInt("comments_count")
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Post findById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, title, text, tags, likes_count, comments_count FROM post WHERE id = ?",
                (rs, rowNum) -> {
                    try {
                        return new Post(
                                rs.getLong("id"),
                                rs.getString("title"),
                                rs.getString("text"),
                                mapper.readValue(
                                        rs.getString("tags"),
                                        mapper.getTypeFactory()
                                                .constructCollectionType(List.class, String.class)
                                ),
                                rs.getInt("likes_count"),
                                rs.getInt("comments_count")
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                id
        );
    }

    @Override
    public Post save(NewPostDto newPostDto) {
        String tagsJson;
        try {
            tagsJson = mapper.writeValueAsString(newPostDto.tags());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO post (title, text, tags) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, newPostDto.title());
            ps.setString(2, newPostDto.text());
            ps.setString(3, tagsJson);
            return ps;
        }, keyHolder);

        return findById(keyHolder.getKey().longValue());
    }

    @Override
    public List<CommentDto> findAllCommentsForPost(long id) {
        return jdbcTemplate.query(
                "SELECT id, text, post_id FROM comment WHERE post_id = ?",
                (rs, rowNum) -> new CommentDto(
                        rs.getLong("id"),
                        rs.getString("text"),
                        rs.getLong("post_id")
                ),
                id
        );
    }

    @Override
    public CommentDto saveComment(CommentDto commentDto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comment (text, post_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, commentDto.text());
            ps.setLong(2, commentDto.postId());
            return ps;
        }, keyHolder);
        return new CommentDto(keyHolder.getKey().longValue(), commentDto.text(), commentDto.postId());
    }

    @Override
    public void deleteComment(long postId, long commentId) {
        jdbcTemplate.update("DELETE FROM comment WHERE id = ? AND post_id = ?", commentId, postId);
    }

    @Override
    public CommentDto updateComment(long postId, long commentId, String text) {
        jdbcTemplate.update(
                "UPDATE comment SET text = ? WHERE id = ? AND post_id = ?",
                text, commentId, postId
        );
        return new CommentDto(commentId, text, postId);
    }

    @Override
    public Post updatePost(UpdatePostDto updatePostDto) {
        String tagsAsJson;
        try {
            tagsAsJson = mapper.writeValueAsString(updatePostDto.tags());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        jdbcTemplate.update(
                "UPDATE post SET title = ?, text = ?, tags = ?, likes_count = 0, comments_count = 0 WHERE id = ?",
                updatePostDto.title(),
                updatePostDto.text(),
                tagsAsJson,
                updatePostDto.id()
        );
        return findById(updatePostDto.id());
    }

    @Override
    public void deletePost(long postId) {
        jdbcTemplate.update("DELETE FROM post WHERE id = ?", postId);
    }
}
