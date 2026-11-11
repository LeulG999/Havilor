package com.leul.havilor.common.ai;

import java.util.List;

// Turns text into vectors (embeddings) for similarity search. An interface with one real implementation
// (VoyageEmbeddingClient) so tests can use a fake: no real API calls in tests.
public interface EmbeddingClient {

    // False when no API key is set: callers then skip indexing and searching instead of failing.
    boolean isConfigured();

    // Texts that will be stored and searched (one vector per text, same order).
    List<float[]> embedDocuments(List<String> texts);

    // The text of a search.
    float[] embedQuery(String text);
}
