package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.Url;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// domain and onBadList are optional: the domain is taken from the address when missing.
// address is deliberately not checked as a valid URL: a malformed link is evidence too.
public record UrlRequest(
        @NotBlank @Size(max = 2048) String address,
        @Size(max = 253) String domain,
        Boolean onBadList
) {

    Url toUrl() {
        return new Url(address, domain, Boolean.TRUE.equals(onBadList));
    }
}
