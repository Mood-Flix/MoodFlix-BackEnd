package com.duck.moodflix.movie.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageDto<T>(
        List<T> content,
        int page, int size,
        long totalElements, int totalPages,
        boolean first, boolean last
) {
    public static <T> PageDto<T> from(Page<T> p) {
        return new PageDto<>(
                p.getContent(),
                p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages(),
                p.isFirst(), p.isLast()
        );
    }
}
