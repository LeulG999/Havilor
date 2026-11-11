package com.leul.havilor.common.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The real client, offline: its answer type reads Voyage's documented response shape, and without a key it
// refuses instead of calling the API.
class VoyageEmbeddingClientTest {

    @Test
    void readsVoyagesResponseShape() {
        String json = """
                {"object": "list", "model": "voyage-3.5", "usage": {"total_tokens": 12},
                 "data": [{"object": "embedding", "index": 1, "embedding": [0.3, 0.4]},
                          {"object": "embedding", "index": 0, "embedding": [0.1, 0.2]}]}""";

        VoyageEmbeddingClient.VoyageResponse response =
                JsonMapper.builder().build().readValue(json, VoyageEmbeddingClient.VoyageResponse.class);

        assertThat(response.data()).hasSize(2);
        assertThat(response.data().get(1).index()).isZero();
        assertThat(response.data().get(1).embedding()).containsExactly(0.1f, 0.2f);
    }

    @Test
    void withoutAKeyItIsNotConfiguredAndNeverCallsTheApi() {
        VoyageEmbeddingClient client = new VoyageEmbeddingClient("", "voyage-3.5");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.embedDocuments(List.of("x"))).isInstanceOf(AiException.class);
    }
}
