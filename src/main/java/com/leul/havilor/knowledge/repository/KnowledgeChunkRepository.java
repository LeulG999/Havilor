package com.leul.havilor.knowledge.repository;

import com.leul.havilor.knowledge.api.response.KnowledgeMatchResponse;
import com.leul.havilor.knowledge.domain.enums.KnowledgeSourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

// Plain SQL through JdbcTemplate, not JPA: the only special thing here is pgvector's `vector` type and its
// `<=>` (cosine distance) operator, which are simplest as SQL. A vector is sent as text, e.g. '[0.1,0.2]',
// and cast with ::vector.
@Repository
public class KnowledgeChunkRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeChunkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean exists(KnowledgeSourceType type, Long sourceId) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM knowledge_chunk WHERE source_type = ? AND source_id = ?)",
                Boolean.class, type.name(), sourceId);
        return Boolean.TRUE.equals(exists);
    }

    // A source already indexed is left as it is (unique source_type + source_id).
    public void insert(KnowledgeSourceType type, Long sourceId, String content, float[] embedding, Instant createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO knowledge_chunk (source_type, source_id, content, embedding, created_at)
                        VALUES (?, ?, ?, ?::vector, ?)
                        ON CONFLICT (source_type, source_id) DO NOTHING""",
                type.name(), sourceId, content, toVectorText(embedding), Timestamp.from(createdAt));
    }

    // The `limit` chunks closest to the query vector (smallest cosine distance first); uses the HNSW index.
    public List<KnowledgeMatchResponse> findNearest(float[] query, int limit) {
        return jdbcTemplate.query("""
                        SELECT source_type, source_id, content FROM knowledge_chunk
                        ORDER BY embedding <=> ?::vector LIMIT ?""",
                (row, n) -> new KnowledgeMatchResponse(row.getString("source_type"), row.getLong("source_id"),
                        row.getString("content")),
                toVectorText(query), limit);
    }

    static String toVectorText(float[] vector) {
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                text.append(',');
            }
            text.append(vector[i]);
        }
        return text.append(']').toString();
    }
}
