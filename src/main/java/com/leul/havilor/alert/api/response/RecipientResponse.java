package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.DeliveryStatus;
import com.leul.havilor.alert.domain.Recipient;

public record RecipientResponse(Long id, String address, DeliveryStatus delivery) {

    static RecipientResponse from(Recipient r) {
        return new RecipientResponse(r.getId(), r.getAddress(), r.getDelivery());
    }
}
