package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecipientRequest(
        @NotBlank @Size(max = 320) String address,
        @NotNull DeliveryStatus delivery
) {
}
