package com.miloo.common.otp;

public enum OtpChannel {
    SMS,
    EMAIL;

    public static OtpChannel fromDestination(String destination) {
        if (destination != null && destination.contains("@")) {
            return EMAIL;
        }
        return SMS;
    }
}
