package com.miloo.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("email")
    private String email;

    @JsonProperty("provider")
    private String provider;
}
