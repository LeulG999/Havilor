package com.leul.havilor.knowledge.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkRepositoryTest {

    // pgvector reads a vector from text like '[0.5,-1.0,2.0]' (cast with ::vector).
    @Test
    void vectorsAreSentToPostgresAsText() {
        assertThat(KnowledgeChunkRepository.toVectorText(new float[]{0.5f, -1f, 2f})).isEqualTo("[0.5,-1.0,2.0]");
    }
}
