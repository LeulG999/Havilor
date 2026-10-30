package com.leul.havilor.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

// The paging rules every list endpoint shares: default size 20, never more than 100.
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    // Out-of-range values are corrected, not rejected: page < 0 → 0, size < 1 → 20, size > 100 → 100.
    public static PageRequest pageRequest(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
