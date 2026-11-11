package com.leul.havilor.common.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// The real EmbeddingClient: Voyage AI's embeddings API (POST /v1/embeddings) through Spring's RestClient.
// Key from VOYAGE_API_KEY (havilor.embedding.api-key), model from application.yml (havilor.embedding.model).
// input_type tells Voyage whether a text is stored ("document") or searched for ("query"); that improves matches.
@Component
class VoyageEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String model;
    private final boolean configured;

    VoyageEmbeddingClient(@Value("${havilor.embedding.api-key}") String apiKey,
                          @Value("${havilor.embedding.model}") String model) {
        this.configured = !apiKey.isBlank();
        this.model = model;
        // Timeouts, so a slow embedding API can't hold a request (e.g. closing an alert) for long.
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(5));
        timeouts.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl("https://api.voyageai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(timeouts)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(texts, "document");
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(List.of(text), "query").getFirst();
    }

    // One request for all texts (the API takes a list), so indexing many texts doesn't hit rate limits.
    private List<float[]> embed(List<String> texts, String inputType) {
        if (!configured) {
            throw new AiException("Embeddings are not configured (VOYAGE_API_KEY is not set)");
        }
        try {
            VoyageResponse response = restClient.post()
                    .uri("/embeddings")
                    .body(Map.of("input", texts, "model", model, "input_type", inputType))
                    .retrieve()
                    .body(VoyageResponse.class);
            if (response == null || response.data() == null || response.data().size() != texts.size()) {
                throw new AiException("The embedding service gave an unexpected answer");
            }
            return response.data().stream()
                    .sorted(Comparator.comparingInt(VoyageResponse.Item::index))
                    .map(VoyageResponse.Item::embedding)
                    .toList();
        } catch (RestClientException e) {
            throw new AiException("The embedding service is not available right now", e);
        }
    }

    // The part of Voyage's answer we use: {"data": [{"index": 0, "embedding": [0.01, ...]}, ...]};
    // other fields ("model", "usage", "object") are ignored.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VoyageResponse(List<Item> data) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Item(int index, float[] embedding) {
        }
    }
}
