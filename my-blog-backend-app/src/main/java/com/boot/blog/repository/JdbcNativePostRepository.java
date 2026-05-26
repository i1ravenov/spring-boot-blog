package com.boot.blog.repository;

import com.boot.blog.dto.CommentDto;
import com.boot.blog.dto.NewPostDto;
import com.boot.blog.dto.UpdatePostDto;
import com.boot.blog.exception.PostNotFoundException;
import com.boot.blog.model.Post;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class JdbcNativePostRepository implements PostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNativePostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── row mapper ────────────────────────────────────────────────────────────

    private RowMapper<Post> postRowMapper() {
        return (rs, rowNum) -> {
            String tagsCsv = rs.getString("tags_csv");
            List<String> tags = (tagsCsv != null && !tagsCsv.isEmpty())
                    ? List.of(tagsCsv.split(","))
                    : List.of();
            return new Post(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("text"),
                    tags,
                    rs.getInt("likes_count"),
                    rs.getInt("comments_count")
            );
        };
    }

    // ── select with tag aggregation ───────────────────────────────────────────

    private static final String SELECT_POSTS =
            "SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, " +
            "LISTAGG(t.name, ',') WITHIN GROUP (ORDER BY t.name) AS tags_csv " +
            "FROM post p " +
            "LEFT JOIN post_tag pt ON p.id = pt.post_id " +
            "LEFT JOIN tag t ON pt.tag_id = t.id ";

    @Override
    public List<Post> findAll(String search, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        if (search != null && !search.isBlank()) {
            return jdbcTemplate.query(
                    SELECT_POSTS +
                    "WHERE p.id IN (" +
                    "  SELECT pt2.post_id FROM post_tag pt2 " +
                    "  JOIN tag t2 ON pt2.tag_id = t2.id " +
                    "  WHERE LOWER(t2.name) = LOWER(?) " +
                    ") " +
                    "GROUP BY p.id, p.title, p.text, p.likes_count, p.comments_count " +
                    "ORDER BY p.id LIMIT ? OFFSET ?",
                    postRowMapper(),
                    search, pageSize, offset);
        }
        return jdbcTemplate.query(
                SELECT_POSTS +
                "GROUP BY p.id, p.title, p.text, p.likes_count, p.comments_count " +
                "ORDER BY p.id LIMIT ? OFFSET ?",
                postRowMapper(),
                pageSize, offset);
    }

    @Override
    public int countAll(String search) {
        if (search != null && !search.isBlank()) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT p.id) FROM post p " +
                    "JOIN post_tag pt ON p.id = pt.post_id " +
                    "JOIN tag t ON pt.tag_id = t.id " +
                    "WHERE LOWER(t.name) = LOWER(?)",
                    Integer.class,
                    search);
            return count != null ? count : 0;
        }
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public Post findById(long id) {
        try {
            return jdbcTemplate.queryForObject(
                    SELECT_POSTS +
                    "WHERE p.id = ? " +
                    "GROUP BY p.id, p.title, p.text, p.likes_count, p.comments_count",
                    postRowMapper(),
                    id);
        } catch (EmptyResultDataAccessException e) {
            throw new PostNotFoundException(id);
        }
    }

    // ── writes ────────────────────────────────────────────────────────────────

    @Override
    public Post save(NewPostDto newPostDto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO post (title, text) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, newPostDto.title());
            ps.setString(2, newPostDto.text());
            return ps;
        }, keyHolder);
        long postId = keyHolder.getKey().longValue();
        saveTags(postId, newPostDto.tags());
        return findById(postId);
    }

    @Override
    public Post updatePost(UpdatePostDto dto) {
        jdbcTemplate.update(
                "UPDATE post SET title = ?, text = ? WHERE id = ?",
                dto.title(), dto.text(), dto.id());
        jdbcTemplate.update("DELETE FROM post_tag WHERE post_id = ?", dto.id());
        saveTags(dto.id(), dto.tags());
        return findById(dto.id());
    }

    @Override
    public void deletePost(long postId) {
        jdbcTemplate.update("DELETE FROM post WHERE id = ?", postId);
    }

    @Override
    public Post incrementLikes(long id) {
        int updated = jdbcTemplate.update(
                "UPDATE post SET likes_count = likes_count + 1 WHERE id = ?", id);
        if (updated == 0) throw new PostNotFoundException(id);
        return findById(id);
    }

    // ── comments ──────────────────────────────────────────────────────────────

    @Override
    public List<CommentDto> findAllCommentsForPost(long id) {
        return jdbcTemplate.query(
                "SELECT id, text, post_id FROM comment WHERE post_id = ?",
                (rs, rowNum) -> new CommentDto(
                        rs.getLong("id"),
                        rs.getString("text"),
                        rs.getLong("post_id")),
                id);
    }

    @Override
    public CommentDto saveComment(CommentDto commentDto) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comment (text, post_id) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
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
                text, commentId, postId);
        return new CommentDto(commentId, text, postId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Upserts each tag into the {@code tag} table (by name), then inserts the
     * corresponding rows into {@code post_tag}.
     */
    private void saveTags(long postId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        for (String name : tags) {
            jdbcTemplate.update("MERGE INTO tag (name) KEY(name) VALUES (?)", name);
            Long tagId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tag WHERE name = ?", Long.class, name);
            jdbcTemplate.update(
                    "INSERT INTO post_tag (post_id, tag_id) VALUES (?, ?)", postId, tagId);
        }
    }
}
