package com.leul.havilor.alert.api.response;

import com.leul.havilor.alert.domain.EmailDetails;
import com.leul.havilor.alert.domain.enums.AuthResult;

public record EmailDetailsResponse(
        String subject,
        String fromAddress,
        String envelopeSender,
        String senderHeader,
        String replyTo,
        String xMailer,
        AuthResult spfResult,
        AuthResult dkimResult,
        AuthResult dmarcResult,
        Integer phishScore,
        Integer spamScore,
        String filterAction,
        String quarantineFolder,
        String rawHeaders
) {

    static EmailDetailsResponse from(EmailDetails email) {
        return new EmailDetailsResponse(
                email.getSubject(),
                email.getFromAddress(),
                email.getEnvelopeSender(),
                email.getSenderHeader(),
                email.getReplyTo(),
                email.getXMailer(),
                email.getSpfResult(),
                email.getDkimResult(),
                email.getDmarcResult(),
                email.getPhishScore(),
                email.getSpamScore(),
                email.getFilterAction(),
                email.getQuarantineFolder(),
                email.getRawHeaders());
    }
}
