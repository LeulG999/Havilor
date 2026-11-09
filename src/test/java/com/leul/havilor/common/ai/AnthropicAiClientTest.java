package com.leul.havilor.common.ai;

import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.leul.havilor.alert.api.response.AiAssessmentResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The real client, without the network: the request it would send is built and checked here.
class AnthropicAiClientTest {

    private final AnthropicAiClient client = new AnthropicAiClient("", "claude-sonnet-5-5");

    @Test
    void aStructuredRequestBuildsWithTheSchemaOfTheAnswerType() {
        StructuredMessageCreateParams<AiAssessmentResponse> params =
                client.structuredParams("Assess this.", AiInput.text("{}"), AiAssessmentResponse.class);

        assertThat(params.rawParams().model().asString()).isEqualTo("claude-sonnet-5-5");
        assertThat(params.rawParams().outputConfig()).isPresent();
    }

    @Test
    void anImageOrAPdfGoesBeforeTheText() {
        List<ContentBlockParam> image = AnthropicAiClient.content(
                AiInput.file("Read this screenshot.", new byte[]{1, 2, 3}, "image/png"));
        List<ContentBlockParam> pdf = AnthropicAiClient.content(
                AiInput.file("Read this PDF.", new byte[]{1, 2, 3}, "application/pdf"));

        assertThat(image).hasSize(2);
        assertThat(image.get(0).isImage()).isTrue();
        assertThat(image.get(1).isText()).isTrue();
        assertThat(pdf.get(0).isDocument()).isTrue();
        assertThat(AnthropicAiClient.content(AiInput.text("only text"))).singleElement()
                .satisfies(block -> assertThat(block.isText()).isTrue());
    }

    @Test
    void withoutAnApiKeyItSaysSoInsteadOfCallingTheApi() {
        assertThatThrownBy(() -> client.text("x", AiInput.text("y")))
                .isInstanceOf(AiException.class)
                .hasMessage("The AI assistant is not configured (ANTHROPIC_API_KEY is not set)");
    }
}
