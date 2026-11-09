package com.leul.havilor;

import com.leul.havilor.common.ai.AiClient;
import com.leul.havilor.common.ai.AiInput;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// Tests never call the real Claude API. AI tests import this: a @Primary fake AiClient that returns whatever the
// test told it to, and remembers what it was asked.
@TestConfiguration(proxyBeanMethods = false)
public class FakeAiConfiguration {

    @Bean
    @Primary
    FakeAiClient fakeAiClient() {
        return new FakeAiClient();
    }

    public static class FakeAiClient implements AiClient {

        private final Map<Class<?>, Supplier<?>> answers = new HashMap<>();
        private Supplier<String> textAnswer = () -> "fake draft";
        public String lastInstructions;
        public AiInput lastInput;

        public <T> void answer(Class<T> type, Supplier<T> answer) {
            answers.put(type, answer);
        }

        public void answerText(Supplier<String> answer) {
            this.textAnswer = answer;
        }

        @Override
        public <T> T structured(String instructions, AiInput input, Class<T> type) {
            remember(instructions, input);
            Supplier<?> answer = answers.get(type);
            if (answer == null) {
                throw new IllegalStateException("The test gave the fake no answer for " + type.getSimpleName());
            }
            return type.cast(answer.get());
        }

        @Override
        public String text(String instructions, AiInput input) {
            remember(instructions, input);
            return textAnswer.get();
        }

        private void remember(String instructions, AiInput input) {
            this.lastInstructions = instructions;
            this.lastInput = input;
        }
    }
}
