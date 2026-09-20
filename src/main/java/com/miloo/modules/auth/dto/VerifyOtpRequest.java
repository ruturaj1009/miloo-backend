package com.miloo.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("country_code")
    private String countryCode;

    @JsonProperty("email")
    private String email;

    @NotBlank(message = "OTP cannot be blank")
    @JsonProperty("otp")
    private String otp;
}
