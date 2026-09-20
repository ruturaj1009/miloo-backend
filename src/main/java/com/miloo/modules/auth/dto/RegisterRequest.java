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
public class RegisterRequest {

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("email")
    private String email;

    @JsonProperty("password")
    private String password;

    @JsonProperty("otp")
    private String otp;

    @JsonProperty("secure_account")
    private Boolean secureAccount;

    @JsonProperty("two_step_enabled")
    private Boolean twoStepEnabled;
}
