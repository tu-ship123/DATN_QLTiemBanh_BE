package com.poly.cake.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Proxy sang chatbot service (Flask + Groq) chạy nội bộ.
 * Frontend KHÔNG gọi thẳng Flask — luôn đi qua backend để:
 *  - Giấu URL + internal token của chatbot service
 *  - Gắn được id khách hàng (nếu đã đăng nhập) làm sessionId cho lịch sử hội thoại
 */
@Slf4j
@Service
public class ChatbotService {

    private final RestTemplate restTemplate;

    @Value("${chatbot.base-url:http://127.0.0.1:5000}")
    private String chatbotBaseUrl;

    @Value("${chatbot.internal-token:}")
    private String internalToken;

    public ChatbotService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, String> ask(String prompt, String sessionId, MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("prompt", prompt == null ? "" : prompt);
            builder.part("sessionId", sessionId == null ? "guest" : sessionId);
            if (file != null && !file.isEmpty()) {
                builder.part("file", file.getResource())
                        .filename(file.getOriginalFilename());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (internalToken != null && !internalToken.isBlank()) {
                headers.set("X-Internal-Token", internalToken);
            }

            HttpEntity<org.springframework.util.MultiValueMap<String, org.springframework.http.HttpEntity<?>>> requestEntity =
                    new HttpEntity<>(builder.build(), headers);

            @SuppressWarnings("unchecked")
            Map<String, String> response = restTemplate.postForObject(
                    chatbotBaseUrl + "/chat", requestEntity, Map.class);

            return response;
        } catch (RestClientException e) {
            log.error("Không gọi được chatbot service tại {}: {}", chatbotBaseUrl, e.getMessage());
            return Map.of("response",
                    "⚠️ Trợ lý ảo hiện không phản hồi được. Bạn vui lòng thử lại sau hoặc dùng mục \"Nhắn tin với cửa hàng\" nhé!");
        }
    }
}
