package com.leul.havilor.knowledge.api.response;

// One search hit. sourceType is a String so other modules can read it without knowledge.domain (the border rule).
public record KnowledgeMatchResponse(String sourceType, Long sourceId, String content) {
}
