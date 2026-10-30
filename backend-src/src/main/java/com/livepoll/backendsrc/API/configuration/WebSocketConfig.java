package com.livepoll.backendsrc.API.configuration;

import com.livepoll.backendsrc.API.service.tokens.TokenService;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenService tokenService;

    @Value("${app.cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    public WebSocketConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
                    return message;
                }

                String token = tokenService.parseTokenFromHeader(firstNativeHeader(accessor, "Authorization"));
                if (token == null || token.isBlank()) {
                    return message;
                }

                JWTClaimsSet claimsSet = tokenService.validateJWTToken(token);
                accessor.setUser(new UsernamePasswordAuthenticationToken(claimsSet.getSubject(), null, List.of()));
                return message;
            }
        });
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://[::1]:*",
                        allowedOrigin)
                .withSockJS();
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.get(0);
    }
}
