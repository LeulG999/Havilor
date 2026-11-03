package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.Recipient;
import com.leul.havilor.alert.domain.enums.DeliveryStatus;

public record RecipientResponse(Long id, String address, DeliveryStatus delivery) {

    static RecipientResponse from(Recipient r) {
        return new RecipientResponse(r.getId(), r.getAddress(), r.getDelivery());
    }
}
