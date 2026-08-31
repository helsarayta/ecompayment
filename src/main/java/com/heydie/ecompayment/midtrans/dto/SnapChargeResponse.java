package com.heydie.ecompayment.midtrans.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnapChargeResponse(
        String token,
        @JsonProperty("redirect_url") String redirectUrl
) {
}
