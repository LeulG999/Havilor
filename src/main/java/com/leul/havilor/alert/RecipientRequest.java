package com.leul.havilor.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecipientRequest(
        @NotBlank @Size(max = 320) String address,
        @NotNull DeliveryStatus delivery
) {

    Recipient toRecipient() {
        return new Recipient(address, delivery);
    }
}
