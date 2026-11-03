package com.leul.havilor.dev;

import com.leul.havilor.alert.ClickRequest;
import com.leul.havilor.alert.CreateAlertRequest;
import com.leul.havilor.alert.RecipientRequest;
import com.leul.havilor.alert.UrlRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FakeAlertFactoryTest {

    // A fixed seed makes the run repeatable: if a generated alert ever fails, it fails the same way again.
    private final FakeAlertFactory factory = new FakeAlertFactory(RandomGenerator.of("L64X128MixRandom"));
    private final List<CreateAlertRequest> alerts = IntStream.range(0, 200).mapToObj(i -> factory.next()).toList();

    @Test
    void everyGeneratedAlertPassesTheApiValidation() {
        try (var factoryOfValidators = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factoryOfValidators.getValidator();
            assertThat(alerts).allSatisfy(alert -> assertThat(validator.validate(alert)).isEmpty());
        }
    }

    @Test
    void everyAddressAndLinkUsesAReservedExampleDomain() {
        // Safety rule: fake data only. RFC 2606 domains can never belong to a real organisation.
        Stream<String> hosts = alerts.stream().flatMap(alert -> Stream.of(
                        Stream.of(alert.email().fromAddress(), alert.email().envelopeSender(), alert.email().replyTo()),
                        alert.recipients().stream().map(RecipientRequest::address),
                        alert.urls().stream().map(u -> URI.create(u.address()).getHost()))
                .flatMap(s -> s))
                .filter(value -> value != null)
                .map(value -> value.substring(value.indexOf('@') + 1));

        assertThat(hosts).allMatch(host -> host.matches("(.+\\.)?example\\.(com|net|org)"));
    }

    @Test
    void clicksOnlyJoinRecipientsAndUrlsOfTheSameAlert() {
        assertThat(alerts).allSatisfy(alert -> {
            List<String> recipients = alert.recipients().stream().map(RecipientRequest::address).toList();
            List<String> urls = alert.urls().stream().map(UrlRequest::address).toList();
            assertThat(alert.clicks()).extracting(ClickRequest::recipientAddress).allMatch(recipients::contains);
            assertThat(alert.clicks()).extracting(ClickRequest::urlAddress).allMatch(urls::contains);
        });
        assertThat(alerts).anyMatch(alert -> !alert.clicks().isEmpty());
    }
}
