package com.leul.havilor.alert.service;

import com.leul.havilor.alert.api.request.AttachmentRequest;
import com.leul.havilor.alert.api.request.ClickRequest;
import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.api.request.EmailDetailsRequest;
import com.leul.havilor.alert.api.request.RecipientRequest;
import com.leul.havilor.alert.api.request.UrlRequest;
import com.leul.havilor.alert.domain.Alert;
import com.leul.havilor.alert.domain.Attachment;
import com.leul.havilor.alert.domain.EmailDetails;
import com.leul.havilor.alert.domain.Recipient;
import com.leul.havilor.alert.domain.Url;
import com.leul.havilor.common.error.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Turns a CreateAlertRequest (plain data) into a new Alert with all its children and clicks.
// This is the only place that knows how request fields map to the domain; the DTOs stay data-only.
@Component
public class AlertFactory {

    // A click must name a recipient and a URL from the same request; anything else is a 400
    // (BadRequestException): the fields are valid one by one, but they don't fit together.
    public Alert create(CreateAlertRequest request) {
        Alert alert = new Alert(request.ticketNumber(), request.source(), request.caseType(), request.priority(),
                request.receivedAt(), emailDetails(request.email()));
        request.recipients().forEach(r -> alert.addRecipient(recipient(r)));
        request.urls().forEach(u -> alert.addUrl(url(u)));
        request.attachments().forEach(a -> alert.addAttachment(attachment(a)));
        addClicks(alert, request.clicks());
        return alert;
    }

    private static void addClicks(Alert alert, List<ClickRequest> clicks) {
        // Email addresses are case-insensitive; URLs are compared exactly.
        Map<String, Recipient> recipientsByAddress = alert.getRecipients().stream()
                .collect(Collectors.toMap(r -> r.getAddress().toLowerCase(Locale.ROOT), Function.identity(),
                        (first, duplicate) -> first));
        Map<String, Url> urlsByAddress = alert.getUrls().stream()
                .collect(Collectors.toMap(Url::getAddress, Function.identity(), (first, duplicate) -> first));
        for (int i = 0; i < clicks.size(); i++) {
            ClickRequest click = clicks.get(i);
            Recipient recipient = recipientsByAddress.get(click.recipientAddress().toLowerCase(Locale.ROOT));
            if (recipient == null) {
                throw new BadRequestException("clicks[" + i + "].recipientAddress", "is not one of the recipients");
            }
            Url url = urlsByAddress.get(click.urlAddress());
            if (url == null) {
                throw new BadRequestException("clicks[" + i + "].urlAddress", "is not one of the urls");
            }
            url.addClick(recipient, click.clickedAt(), click.permitted());
        }
    }

    private static EmailDetails emailDetails(EmailDetailsRequest email) {
        return EmailDetails.builder()
                .subject(email.subject())
                .fromAddress(email.fromAddress())
                .envelopeSender(email.envelopeSender())
                .senderHeader(email.senderHeader())
                .replyTo(email.replyTo())
                .xMailer(email.xMailer())
                .spfResult(email.spfResult())
                .dkimResult(email.dkimResult())
                .dmarcResult(email.dmarcResult())
                .phishScore(email.phishScore())
                .spamScore(email.spamScore())
                .filterAction(email.filterAction())
                .quarantineFolder(email.quarantineFolder())
                .rawHeaders(email.rawHeaders())
                .build();
    }

    private static Recipient recipient(RecipientRequest r) {
        return new Recipient(r.address(), r.delivery());
    }

    private static Url url(UrlRequest u) {
        return new Url(u.address(), u.domain(), Boolean.TRUE.equals(u.onBadList()));
    }

    private static Attachment attachment(AttachmentRequest a) {
        return new Attachment(a.fileName(), a.fileType(), a.sha256(), a.verdict());
    }
}
