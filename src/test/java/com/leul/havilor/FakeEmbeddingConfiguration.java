package com.leul.havilor;

import com.leul.havilor.common.ai.AiException;
import com.leul.havilor.common.ai.EmbeddingClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Locale;

// Tests never call the real Voyage API. Knowledge tests import this: a @Primary fake EmbeddingClient that turns a
// text into a 1024-number vector from its words (a "bag of words"), so texts sharing words come out similar.
// Deterministic, offline, and it can be told to fail.
@TestConfiguration(proxyBeanMethods = false)
public class FakeEmbeddingConfiguration {

    public static final int DIMENSIONS = 1024; // must match vector(1024) in V16

    @Bean
    @Primary
    FakeEmbeddingClient fakeEmbeddingClient() {
        return new FakeEmbeddingClient();
    }

    public static class FakeEmbeddingClient implements EmbeddingClient {

        public volatile boolean failing;

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public List<float[]> embedDocuments(List<String> texts) {
            return texts.stream().map(this::embed).toList();
        }

        @Override
        public float[] embedQuery(String text) {
            return embed(text);
        }

        private float[] embed(String text) {
            if (failing) {
                throw new AiException("The embedding service is not available right now");
            }
            float[] vector = new float[DIMENSIONS];
            for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
                if (word.length() > 2) {
                    vector[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1;
                }
            }
            vector[0] += 0.01f; // never the zero vector (cosine distance is undefined for it)
            return vector;
        }
    }
}
