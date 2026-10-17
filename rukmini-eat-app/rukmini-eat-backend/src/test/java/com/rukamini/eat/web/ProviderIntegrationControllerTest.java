package com.rukamini.eat.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderIntegrationControllerTest {

    @Test
    void providersRemainUnavailableUntilApprovedAdaptersAreImplemented() {
        var statuses = new ProviderIntegrationController().providers();
        assertEquals(List.of("zomato", "swiggy"),
                statuses.stream().map(s -> s.provider()).toList());
        statuses.forEach(status -> {
            assertEquals("AWAITING_APPROVED_API_CONTRACT", status.status());
            assertFalse(status.liveData());
            assertFalse(status.realOrdering());
        });
    }
}
