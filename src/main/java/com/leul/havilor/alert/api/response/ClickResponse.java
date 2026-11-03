package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.ClickEvent;
import java.time.Instant;

public record ClickResponse(Long id, String recipientAddress, String urlAddress, Instant clickedAt, boolean permitted) {

    static ClickResponse from(ClickEvent click) {
        return new ClickResponse(click.getId(), click.getRecipient().getAddress(), click.getUrl().getAddress(),
                click.getClickedAt(), click.isPermitted());
    }
}
