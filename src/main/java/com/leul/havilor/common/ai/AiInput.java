package com.leul.havilor.common.ai;

// What Claude reads: always some text, and optionally one file (an image or a PDF) for Claude's vision.
public record AiInput(String text, byte[] file, String mediaType) {

    public static AiInput text(String text) {
        return new AiInput(text, null, null);
    }

    // mediaType: image/png, image/jpeg, image/gif, image/webp or application/pdf.
    public static AiInput file(String text, byte[] file, String mediaType) {
        return new AiInput(text, file, mediaType);
    }

    public boolean hasFile() {
        return file != null;
    }
}
