package com.miloo.modules.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingPayloadDto {

    private String type; // OFFER, ANSWER, ICE_CANDIDATE, CALL_INVITE, CALL_ACCEPT, CALL_REJECT, CALL_END
    private String sessionId;
    private String senderId;
    private String targetId;
    private Object sdp;
    private Object candidate;
    private String callType; // AUDIO, VIDEO
}
