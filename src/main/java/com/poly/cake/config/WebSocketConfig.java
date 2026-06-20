package com.poly.cake.config;

import com.poly.cake.security.JwtUtil;
import com.poly.cake.service.RedisTokenService;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.ArrayList;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil; 
    
    @Autowired
    private RedisTokenService redisTokenService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mở cổng kết nối cho Frontend VueJS. Dùng setAllowedOriginPatterns để tránh lỗi CORS
        registry.addEndpoint("/ws-bakery")
                .setAllowedOriginPatterns("*")
                .withSockJS(); 
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Kênh để server BẮN data về cho Frontend đăng ký lắng nghe
        registry.enableSimpleBroker("/topic", "/user");
        
        // Tiền tố khi Frontend GỬI data lên Server
        registry.setApplicationDestinationPrefixes("/app");
        
        // Cấu hình tiền tố cho kênh cá nhân (ví dụ gửi riêng cho 1 user)
        registry.setUserDestinationPrefix("/user");
    }

    // TRẠM GÁC: Bắt Token JWT từ quá trình Handshake kết nối WebSocket
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                // Khi client yêu cầu CONNECT
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        
                        try {
                            // Kiểm tra Blacklist và tính hợp lệ của Token
                            if (!redisTokenService.isTokenBlacklisted(token) && jwtUtil.isTokenValid(token)) {
                                
                                // Dùng JwtUtil lấy email
                                String userEmail = jwtUtil.extractEmail(token); 
                                
                                if (userEmail != null) {
                                    UsernamePasswordAuthenticationToken authentication = 
                                            new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                    accessor.setUser(authentication);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi xác thực Token WebSocket: " + e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}