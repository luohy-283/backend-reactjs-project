package com.company.bookingroom.config;

import static com.company.bookingroom.security.SecurityUtils.AUTHORITIES_CLAIM;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket for realtime notification push.
 * HTTP handshake on {@code /ws} is permitAll; JWT is required on STOMP CONNECT.
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket for @stomp/stompjs; SockJS kept for older clients.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(
            new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                        authenticateConnect(accessor);
                    }
                    return message;
                }
            }
        );
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header on STOMP CONNECT");
        }
        String token = authorization.substring(7).trim();
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Collection<? extends GrantedAuthority> authorities = authoritiesFromJwt(jwt);
            var authentication = new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
            accessor.setUser(authentication);
        } catch (JwtException ex) {
            LOG.debug("STOMP CONNECT JWT rejected: {}", ex.getMessage());
            throw new IllegalArgumentException("Invalid JWT on STOMP CONNECT", ex);
        }
    }

    private static List<GrantedAuthority> authoritiesFromJwt(Jwt jwt) {
        String claim = jwt.getClaimAsString(AUTHORITIES_CLAIM);
        if (!StringUtils.hasText(claim)) {
            return List.of();
        }
        return Arrays.stream(claim.split(" "))
            .filter(StringUtils::hasText)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
}
