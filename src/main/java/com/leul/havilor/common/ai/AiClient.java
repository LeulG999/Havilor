package com.leul.havilor.common.ai;

// The only door to the LLM (CLAUDE.md: "services talk to the LLM only through an AI client").
// An interface with one real implementation (AnthropicAiClient) so tests can plug in a fake: no real API calls in tests.
public interface AiClient {

    // Claude answers with an instance of `type`: the SDK turns the class into a JSON schema and the API's structured
    // output guarantees valid JSON for it.
    <T> T structured(String instructions, AiInput input, Class<T> type);

    // A plain text answer (e.g. an email draft).
    String text(String instructions, AiInput input);
}
