package com.leul.havilor.common;

import org.springframework.data.domain.Page;

import java.util.List;

// Our own page shape for every list endpoint. We don't return Spring's Page directly:
// its JSON has many internal fields and could change between Spring versions.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
