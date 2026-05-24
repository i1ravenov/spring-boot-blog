package com.boot.blog.dto;

import java.util.List;

public record UpdatePostDto(long id, String title, String text, List<String> tags) {
}
