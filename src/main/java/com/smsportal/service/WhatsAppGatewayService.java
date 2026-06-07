package com.smsportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends WhatsApp messages via the Meta (Facebook) Cloud API.
 *
 * Docs: https://developers.facebook.com/docs/whatsapp/cloud-api
 *
 * Setup:
 *  1. Create a Meta App at https://developers.facebook.com
 *  2. Add "WhatsApp Business" product
 *  3. Get a temporary or permanent access token
 *  4. Get your Phone Number ID from the WhatsApp dashboard
 *  5. Set the values below in application.yml
 */
@Service
@Slf4j
public class WhatsAppGatewayService {

    @Value("${whatsapp.access-token}")
    private String accessToken;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.api-version:v19.0}")
    private String apiVersion;

    @Value("${whatsapp.mock:false}")
    private boolean mockMode;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Text Message ─────────────────────────────────────────────────

    public String sendTextMessage(String toMobile, String message) {
        if (mockMode) return mock("text", toMobile, message);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", normalise(toMobile));
        body.put("type", "text");
        body.put("text", Map.of("preview_url", false, "body", message));

        return callApi(body);
    }

    // ── Template Message ─────────────────────────────────────────────

    public String sendTemplateMessage(String toMobile, String templateName,
                                      String languageCode, List<String> params) {
        if (mockMode) return mock("template:" + templateName, toMobile, "");

        List<Map<String, Object>> components = List.of();

        if (params != null && !params.isEmpty()) {
            List<Map<String, String>> paramObjects = params.stream()
                    .map(p -> Map.of("type", "text", "text", p))
                    .toList();
            components = List.of(Map.of(
                    "type", "body",
                    "parameters", paramObjects
            ));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalise(toMobile));
        body.put("type", "template");
        body.put("template", Map.of(
                "name", templateName,
                "language", Map.of("code", languageCode),
                "components", components
        ));

        return callApi(body);
    }

    // ── Image Message ────────────────────────────────────────────────

    public String sendImageMessage(String toMobile, String imageUrl, String caption) {
        if (mockMode) return mock("image", toMobile, imageUrl);

        Map<String, Object> imageObj = new HashMap<>();
        imageObj.put("link", imageUrl);
        if (caption != null && !caption.isBlank()) imageObj.put("caption", caption);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalise(toMobile));
        body.put("type", "image");
        body.put("image", imageObj);

        return callApi(body);
    }

    // ── Document Message ─────────────────────────────────────────────

    public String sendDocumentMessage(String toMobile, String docUrl,
                                      String filename, String caption) {
        if (mockMode) return mock("document", toMobile, docUrl);

        Map<String, Object> docObj = new HashMap<>();
        docObj.put("link", docUrl);
        if (filename != null) docObj.put("filename", filename);
        if (caption != null) docObj.put("caption", caption);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalise(toMobile));
        body.put("type", "document");
        body.put("document", docObj);

        return callApi(body);
    }

    // ── Video Message ────────────────────────────────────────────────

    public String sendVideoMessage(String toMobile, String videoUrl, String caption) {
        if (mockMode) return mock("video", toMobile, videoUrl);

        Map<String, Object> videoObj = new HashMap<>();
        videoObj.put("link", videoUrl);
        if (caption != null) videoObj.put("caption", caption);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", normalise(toMobile));
        body.put("type", "video");
        body.put("video", videoObj);

        return callApi(body);
    }

    // ── Internal ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callApi(Map<String, Object> body) {
        String url = String.format(
                "https://graph.facebook.com/%s/%s/messages",
                apiVersion, phoneNumberId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> messages =
                        (List<Map<String, Object>>) response.getBody().get("messages");
                if (messages != null && !messages.isEmpty()) {
                    return (String) messages.get(0).get("id");
                }
            }
            throw new RuntimeException("Unexpected API response: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("WhatsApp API error: {}", e.getMessage());
            throw new RuntimeException("WhatsApp send failed: " + e.getMessage());
        }
    }

    /** Ensure mobile has country code prefix, no + */
    private String normalise(String mobile) {
        mobile = mobile.trim().replaceAll("[^0-9]", "");
        if (mobile.length() == 10) mobile = "91" + mobile; // default India
        return mobile;
    }

    private String mock(String type, String to, String content) {
        String fakeId = "MOCK_WA_" + System.currentTimeMillis();
        log.info("[MOCK WA] type={} to={} content={} → id={}", type, to, content, fakeId);
        return fakeId;
    }
}
