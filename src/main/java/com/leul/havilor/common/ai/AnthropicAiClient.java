package com.leul.havilor.common.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.Base64PdfSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// The real AiClient: Claude through the official Anthropic Java SDK (Messages API, one request per call).
// Key from ANTHROPIC_API_KEY (havilor.ai.api-key), model from application.yml (havilor.ai.model).
@Component
class AnthropicAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAiClient.class);
    private static final long MAX_TOKENS = 16_000L;

    private final AnthropicClient client;
    private final String model;
    private final boolean configured;

    AnthropicAiClient(@Value("${havilor.ai.api-key}") String apiKey, @Value("${havilor.ai.model}") String model) {
        this.configured = !apiKey.isBlank();
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public <T> T structured(String instructions, AiInput input, Class<T> type) {
        StructuredMessageCreateParams<T> params = structuredParams(instructions, input, type);
        StructuredMessage<T> message = call(() -> client.messages().create(params));
        failIfRefused(message.stopReason());
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new AiException("The AI assistant gave no answer"));
    }

    @Override
    public String text(String instructions, AiInput input) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(instructions)
                .addUserMessageOfContentBlockParams(content(input))
                .build();
        Message message = call(() -> client.messages().create(params));
        failIfRefused(message.stopReason());
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .reduce("", String::concat)
                .strip();
    }

    // Package-private so a test can check that the request (and the JSON schema derived from `type`) builds,
    // without calling the API.
    <T> StructuredMessageCreateParams<T> structuredParams(String instructions, AiInput input, Class<T> type) {
        return MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(instructions)
                .outputConfig(type)
                .addUserMessageOfContentBlockParams(content(input))
                .build();
    }

    // The file block (if any) goes before the text, as the API recommends.
    static List<ContentBlockParam> content(AiInput input) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        if (input.hasFile()) {
            String data = Base64.getEncoder().encodeToString(input.file());
            if ("application/pdf".equals(input.mediaType())) {
                blocks.add(ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                        .source(Base64PdfSource.builder().data(data).build())
                        .build()));
            } else {
                blocks.add(ContentBlockParam.ofImage(ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .data(data)
                                .mediaType(Base64ImageSource.MediaType.of(input.mediaType()))
                                .build())
                        .build()));
            }
        }
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(input.text()).build()));
        return blocks;
    }

    private <R> R call(Supplier<R> request) {
        if (!configured) {
            throw new AiException("The AI assistant is not configured (ANTHROPIC_API_KEY is not set)");
        }
        try {
            return request.get();
        } catch (AnthropicException e) {
            log.warn("Claude API call failed", e);
            throw new AiException("The AI assistant is not available right now", e);
        }
    }

    // A safety classifier can decline (HTTP 200, stop reason "refusal"): then there is no usable answer.
    private static void failIfRefused(Optional<StopReason> stopReason) {
        if (stopReason.filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new AiException("The AI assistant declined this request");
        }
    }
}
