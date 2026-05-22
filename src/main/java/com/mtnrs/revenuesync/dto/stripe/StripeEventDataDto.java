package com.mtnrs.revenuesync.dto.stripe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeEventDataDto(
        @JsonProperty("object") StripeObjectDto object
) {}
