package com.leul.havilor.alert;

public record AttachmentResponse(Long id, String fileName, String fileType, String sha256, Verdict verdict) {

    static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getFileType(), a.getSha256(), a.getVerdict());
    }
}
