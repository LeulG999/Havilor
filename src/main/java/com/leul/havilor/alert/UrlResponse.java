package com.leul.havilor.alert;

public record UrlResponse(Long id, String address, String domain, boolean onBadList) {

    static UrlResponse from(Url u) {
        return new UrlResponse(u.getId(), u.getAddress(), u.getDomain(), u.isOnBadList());
    }
}
