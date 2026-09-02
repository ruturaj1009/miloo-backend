package com.miloo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (!StringUtils.hasText(bearerToken)) {
                // Support token passed via query param or header
                bearerToken = accessor.getFirstNativeHeader("token");
            }

            if (StringUtils.hasText(bearerToken)) {
                if (bearerToken.startsWith("Bearer ")) {
                    bearerToken = bearerToken.substring(7);
                }

                if (tokenProvider.validateToken(bearerToken)) {
                    UUID userId = tokenProvider.getUserIdFromToken(bearerToken);
                    Principal principal = () -> userId.toString();
                    accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
                    log.info("[WebSocket] Authenticated user: {}", userId);
                } else {
                    log.warn("[WebSocket] Invalid JWT token in STOMP connect header");
                }
            } else {
                log.warn("[WebSocket] Missing Authorization header in STOMP connect frame");
            }
        }

        return message;
    }
}
