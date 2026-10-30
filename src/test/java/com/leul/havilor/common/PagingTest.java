package com.leul.havilor.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagingTest {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "receivedAt");

    @Test
    void keepsValidPageAndSize() {
        Pageable pageable = Paging.pageRequest(3, 50, NEWEST_FIRST);

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort()).isEqualTo(NEWEST_FIRST);
    }

    @Test
    void capsSizeAtMaximum() {
        assertThat(Paging.pageRequest(0, 500, NEWEST_FIRST).getPageSize()).isEqualTo(Paging.MAX_SIZE);
        assertThat(Paging.pageRequest(0, 100, NEWEST_FIRST).getPageSize()).isEqualTo(100);
    }

    @Test
    void usesDefaultSizeWhenSizeIsBelowOne() {
        assertThat(Paging.pageRequest(0, 0, NEWEST_FIRST).getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
        assertThat(Paging.pageRequest(0, -5, NEWEST_FIRST).getPageSize()).isEqualTo(Paging.DEFAULT_SIZE);
    }

    @Test
    void usesFirstPageWhenPageIsNegative() {
        assertThat(Paging.pageRequest(-1, 20, NEWEST_FIRST).getPageNumber()).isZero();
    }

    @Test
    void pageResponseCopiesSpringPageFields() {
        // 45 items in total, we are on page 1 (the second page) of size 20.
        PageImpl<String> springPage = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 20), 45);

        PageResponse<String> response = PageResponse.from(springPage);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(45);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
