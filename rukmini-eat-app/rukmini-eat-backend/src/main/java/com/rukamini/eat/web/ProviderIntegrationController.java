package com.rukamini.eat.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
@Tag(name = "Provider integrations")
public class ProviderIntegrationController {

    public record ProviderStatus(String provider, String status, boolean liveData, boolean realOrdering) {}

    @GetMapping("/providers")
    @Operation(summary = "Get provider integration readiness",
            description = "Static implementation status, not a provider health check. "
                    + "Live data and real ordering remain disabled.")
    @ApiResponse(responseCode = "200", description = "Zomato and Swiggy readiness status")
    @ApiResponse(responseCode = "401", description = "Missing, invalid, or expired token", content = @Content)
    public List<ProviderStatus> providers() {
        return List.of(
                new ProviderStatus("zomato", "AWAITING_APPROVED_API_CONTRACT", false, false),
                new ProviderStatus("swiggy", "AWAITING_APPROVED_API_CONTRACT", false, false));
    }
}
