package com.rukamini.eat.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
public class ProviderIntegrationController {

    public record ProviderStatus(String provider, String status, boolean liveData, boolean realOrdering) {}

    @GetMapping("/providers")
    public List<ProviderStatus> providers() {
        return List.of(
                new ProviderStatus("zomato", "AWAITING_APPROVED_API_CONTRACT", false, false),
                new ProviderStatus("swiggy", "AWAITING_APPROVED_API_CONTRACT", false, false));
    }
}
