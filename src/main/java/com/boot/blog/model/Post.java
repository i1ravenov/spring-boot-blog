package com.boot.blog.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Post {
    private final long id;
    private final String title;
    private final String text;
    private final List<String> tags;
    private final int likesCount;
    private final int commentsCount;
}
