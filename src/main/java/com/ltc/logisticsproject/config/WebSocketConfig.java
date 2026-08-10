package com.ltc.logisticsproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// Stage 6 — STOMP-over-SockJS broadcast channel. Server-push only for now
// (no @MessageMapping handlers): drivers report position/status via the
// existing REST endpoints, and TripBroadcastService fans each update out
// over these topics so the Dispatcher Control Tower and customer live-
// tracking pages update instantly instead of on a 20s poll.
//
//   /topic/dispatcher/live-trips   -> LiveTripResponse, one message per trip update
//   /topic/tracking/{trackingNum}  -> lightweight tracking payload for one cargo
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
