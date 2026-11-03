package com.leul.havilor.alert;

public record RecipientResponse(Long id, String address, DeliveryStatus delivery) {

    static RecipientResponse from(Recipient r) {
        return new RecipientResponse(r.getId(), r.getAddress(), r.getDelivery());
    }
}
