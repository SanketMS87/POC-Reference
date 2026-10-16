package com.rukamini.mail.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Typed response model for the Deal API {@code GET /external/api/Deals/{id}} endpoint.
 *
 * <p>Mirrors (a subset of) the {@code DealAPIModel} schema defined in {@code deal_API.json}.
 * Only the fields actually consumed by the application are declared; unknown properties present
 * in the upstream payload are ignored rather than causing deserialization to fail.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealAPIModel {

    private String insuredName;

    private String underwriterId;

    private String underwriterName;

    private String uwAssistantId;
}
