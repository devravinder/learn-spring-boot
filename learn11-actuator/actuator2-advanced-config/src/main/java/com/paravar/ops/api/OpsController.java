package com.paravar.ops.api;

import com.paravar.ops.features.FeatureFlags;
import com.paravar.ops.health.PaymentGatewayHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class OpsController {

    private static final Logger log = LoggerFactory.getLogger(OpsController.class);

    private final FeatureFlags flags;
    private final PaymentGatewayHealthIndicator gateway;
    private final ApplicationEventPublisher events;

    public OpsController(FeatureFlags flags, PaymentGatewayHealthIndicator gateway, ApplicationEventPublisher events) {
        this.flags = flags;
        this.gateway = gateway;
        this.events = events;
    }

    @GetMapping("/checkout")
    public Map<String, String> checkout() {
        log.debug("checkout requested, new-checkout={}", flags.isOn("new-checkout"));
        return Map.of("flow", flags.isOn("new-checkout") ? "new" : "classic");
    }

    @PostMapping("/payment-gateway/{state}")
    public Map<String, String> gateway(@PathVariable String state) {
        gateway.setReachable(state.equals("up"));
        return Map.of("paymentGateway", state);
    }

    // readiness goes DOWN (503) so a load balancer stops sending traffic; liveness stays UP
    @PostMapping("/maintenance/{state}")
    public Map<String, String> maintenance(@PathVariable String state) {
        ReadinessState readiness = state.equals("on") ? ReadinessState.REFUSING_TRAFFIC : ReadinessState.ACCEPTING_TRAFFIC;
        AvailabilityChangeEvent.publish(events, this, readiness);
        return Map.of("readiness", readiness.name());
    }
}
