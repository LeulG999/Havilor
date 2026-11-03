package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.Verdict;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AttachmentRequest(
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 100) String fileType,
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "must be 64 hex characters") String sha256,
        Verdict verdict
) {
}
