package com.leul.havilor.common.ai;

// The AI assistant could not give an answer: no API key, the API is down or rate-limited, or Claude declined.
// The global handler turns it into 502; the message is safe to show (no API details).
public class AiException extends RuntimeException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
