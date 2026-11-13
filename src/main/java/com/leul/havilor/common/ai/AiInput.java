package com.leul.havilor.common.ai;

import java.util.List;

// What Claude reads: always some text, and optionally files (images or PDFs) for Claude's vision, all in ONE request.
public record AiInput(String text, List<AiFile> files) {

    // One image or PDF. mediaType: image/png, image/jpeg, image/gif, image/webp or application/pdf.
    public record AiFile(byte[] data, String mediaType) {
    }

    public AiInput {
        files = files == null ? List.of() : List.copyOf(files);
    }

    public static AiInput text(String text) {
        return new AiInput(text, List.of());
    }

    public static AiInput file(String text, byte[] file, String mediaType) {
        return new AiInput(text, List.of(new AiFile(file, mediaType)));
    }

    public static AiInput files(String text, List<AiFile> files) {
        return new AiInput(text, files);
    }

    public boolean hasFile() {
        return !files.isEmpty();
    }
}
