package com.leul.havilor.alert.dev;

import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.service.AlertService;
import com.leul.havilor.alert.service.CreateAlertResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

// Fills the queue with fake alerts for demos and manual testing. @Profile("dev"): the bean only exists when
// the app runs with the dev profile, so in any other environment this URL doesn't exist at all (404).
// It goes through AlertService.create, exactly like a real alert from ServiceNow would.
@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev")
public class DevAlertController {

    private final AlertService alertService;
    private final FakeAlertFactory factory;

    public DevAlertController(AlertService alertService, Clock clock) {
        this.alertService = alertService;
        // SplittableRandom lives in java.base. RandomGenerator.getDefault() needs the jdk.random module, which the
        // slim JRE image (Dockerfile) doesn't have: the dev profile failed to start in the container (Flow 24).
        this.factory = new FakeAlertFactory(new SplittableRandom(), clock);
    }

    @PostMapping("/generate-alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GeneratedAlert> generate(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    CreateAlertRequest request = factory.next();
                    CreateAlertResult result = alertService.create(request);
                    return new GeneratedAlert(result.alert().id(), result.alert().ticketNumber(),
                            request.caseType().name(), request.email().subject());
                })
                .toList();
    }

    public record GeneratedAlert(Long id, String ticketNumber, String caseType, String subject) {
    }
}
