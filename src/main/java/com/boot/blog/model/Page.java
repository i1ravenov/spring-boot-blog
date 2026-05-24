package com.boot.blog.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class Page {
    private List<Post> posts;
    private boolean hasPrev;
    private boolean hasNext;
    private int lastPage;
}
