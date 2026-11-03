package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.Url;

public record UrlResponse(Long id, String address, String domain, boolean onBadList) {

    static UrlResponse from(Url u) {
        return new UrlResponse(u.getId(), u.getAddress(), u.getDomain(), u.isOnBadList());
    }
}
