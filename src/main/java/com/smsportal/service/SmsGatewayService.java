package com.smsportal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsGatewayService {

    @Value("${msg91.auth-key}")
    private String msg91AuthKey;

    @Value("${msg91.sender-id}")
    private String defaultSenderId;

    @Value("${app.gateway}")
    private String activeGateway;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send SMS via configured gateway.
     * Returns gateway message ID on success.
     */
    public String sendSms(String mobile, String message, String senderId) {
        return switch (activeGateway.toLowerCase()) {
            case "msg91" -> sendViaMSG91(mobile, message, senderId);
            case "textlocal" -> sendViaTextLocal(mobile, message, senderId);
            default -> sendViaMSG91(mobile, message, senderId);
        };
    }

    private String sendViaMSG91(String mobile, String message, String senderId) {
        try {
            String url = "https://api.msg91.com/api/v5/flow/";

            HttpHeaders headers = new HttpHeaders();
            headers.set("authkey", msg91AuthKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", senderId != null ? senderId : defaultSenderId);
            body.put("message", message);
            body.put("mobiles", "91" + mobile);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object requestId = response.getBody().get("request_id");
                return requestId != null ? requestId.toString() : "MSG91_" + System.currentTimeMillis();
            }

            throw new RuntimeException("MSG91 returned non-200 status: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("MSG91 send failed for {}: {}", mobile, e.getMessage());
            throw new RuntimeException("Gateway error: " + e.getMessage());
        }
    }

    private String sendViaTextLocal(String mobile, String message, String senderId) {
        // TextLocal integration placeholder
        log.warn("TextLocal gateway not fully configured, using mock response");
        return "TL_" + System.currentTimeMillis();
    }

    /**
     * Mock gateway for testing/development without real API keys.
     */
    public String sendMock(String mobile, String message) {
        log.info("[MOCK SMS] To: {} | Message: {}", mobile, message);
        return "MOCK_" + System.currentTimeMillis();
    }
}
