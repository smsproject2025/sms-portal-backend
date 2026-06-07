package com.smsportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smsportal.config.RabbitMQConfig;
import com.smsportal.dto.ApiResponse;
import com.smsportal.model.*;
import com.smsportal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final WhatsAppLogRepository waLogRepository;
    private final WhatsAppTemplateRepository waTemplateRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final WhatsAppGatewayService gatewayService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.whatsapp-rate-text:0.40}")
    private double rateText;

    @Value("${app.whatsapp-rate-template:0.55}")
    private double rateTemplate;

    @Value("${app.whatsapp-rate-media:0.60}")
    private double rateMedia;

    // ── Send Text ────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Map<String, Object>> sendText(List<String> mobiles,
                                                      String message,
                                                      String email) {
        User user = getUser(email);
        double totalCost = rateText * mobiles.size();
        checkAndDeduct(user, totalCost);

        String batchId = UUID.randomUUID().toString();
        List<Long> logIds = new ArrayList<>();

        for (String mobile : mobiles) {
            WhatsAppLog log = WhatsAppLog.builder()
                    .user(user).mobile(clean(mobile))
                    .messageType(WhatsAppLog.MessageType.TEXT)
                    .message(message)
                    .status(WhatsAppLog.MessageStatus.QUEUED)
                    .cost(rateText).batchId(batchId).build();
            log = waLogRepository.save(log);
            logIds.add(log.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SMS_EXCHANGE, "wa.send", log.getId());
        }

        Wallet wallet = walletRepository.findByUser(user).orElseThrow();
        return ApiResponse.success(Map.of(
                "status", "queued",
                "batchId", batchId,
                "totalNumbers", mobiles.size(),
                "totalCost", totalCost,
                "remainingBalance", wallet.getBalance()
        ), "WhatsApp messages queued");
    }

    // ── Send Template ────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Map<String, Object>> sendTemplate(List<String> mobiles,
                                                          String templateName,
                                                          String language,
                                                          List<String> params,
                                                          String email) {
        User user = getUser(email);
        double totalCost = rateTemplate * mobiles.size();
        checkAndDeduct(user, totalCost);

        String batchId = UUID.randomUUID().toString();
        List<Long> logIds = new ArrayList<>();
        String paramsJson = toJson(params);

        for (String mobile : mobiles) {
            WhatsAppLog log = WhatsAppLog.builder()
                    .user(user).mobile(clean(mobile))
                    .messageType(WhatsAppLog.MessageType.TEMPLATE)
                    .templateName(templateName).templateLanguage(language)
                    .templateParams(paramsJson)
                    .status(WhatsAppLog.MessageStatus.QUEUED)
                    .cost(rateTemplate).batchId(batchId).build();
            log = waLogRepository.save(log);
            logIds.add(log.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SMS_EXCHANGE, "wa.send", log.getId());
        }

        Wallet wallet = walletRepository.findByUser(user).orElseThrow();
        return ApiResponse.success(Map.of(
                "status", "queued",
                "batchId", batchId,
                "totalNumbers", mobiles.size(),
                "totalCost", totalCost,
                "remainingBalance", wallet.getBalance()
        ), "Template messages queued");
    }

    // ── Send Media ───────────────────────────────────────────────────

    @Transactional
    public ApiResponse<Map<String, Object>> sendMedia(List<String> mobiles,
                                                       String mediaUrl,
                                                       WhatsAppLog.MessageType mediaType,
                                                       String caption,
                                                       String filename,
                                                       String email) {
        User user = getUser(email);
        double totalCost = rateMedia * mobiles.size();
        checkAndDeduct(user, totalCost);

        String batchId = UUID.randomUUID().toString();

        for (String mobile : mobiles) {
            WhatsAppLog log = WhatsAppLog.builder()
                    .user(user).mobile(clean(mobile))
                    .messageType(mediaType)
                    .mediaUrl(mediaUrl).mediaCaption(caption)
                    .mediaFilename(filename)
                    .status(WhatsAppLog.MessageStatus.QUEUED)
                    .cost(rateMedia).batchId(batchId).build();
            log = waLogRepository.save(log);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SMS_EXCHANGE, "wa.send", log.getId());
        }

        Wallet wallet = walletRepository.findByUser(user).orElseThrow();
        return ApiResponse.success(Map.of(
                "status", "queued",
                "batchId", batchId,
                "totalNumbers", mobiles.size(),
                "totalCost", totalCost,
                "remainingBalance", wallet.getBalance()
        ), "Media messages queued");
    }

    // ── RabbitMQ Consumer ────────────────────────────────────────────

    @RabbitListener(queues = "wa.queue")
    public void processFromQueue(Long logId) {
        WhatsAppLog waLog = waLogRepository.findById(logId).orElse(null);
        if (waLog == null) return;

        try {
            String waId = switch (waLog.getMessageType()) {
                case TEXT     -> gatewayService.sendTextMessage(waLog.getMobile(), waLog.getMessage());
                case TEMPLATE -> {
                    List<String> params = fromJson(waLog.getTemplateParams());
                    yield gatewayService.sendTemplateMessage(
                            waLog.getMobile(), waLog.getTemplateName(),
                            waLog.getTemplateLanguage(), params);
                }
                case IMAGE    -> gatewayService.sendImageMessage(
                        waLog.getMobile(), waLog.getMediaUrl(), waLog.getMediaCaption());
                case DOCUMENT -> gatewayService.sendDocumentMessage(
                        waLog.getMobile(), waLog.getMediaUrl(),
                        waLog.getMediaFilename(), waLog.getMediaCaption());
                case VIDEO    -> gatewayService.sendVideoMessage(
                        waLog.getMobile(), waLog.getMediaUrl(), waLog.getMediaCaption());
                default       -> gatewayService.sendTextMessage(waLog.getMobile(), "");
            };

            waLog.setStatus(WhatsAppLog.MessageStatus.SENT);
            waLog.setWaMessageId(waId);
            waLog.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            waLog.setStatus(WhatsAppLog.MessageStatus.FAILED);
            waLog.setErrorMessage(e.getMessage());
            log.error("WA send failed for {}: {}", waLog.getMobile(), e.getMessage());
        }

        waLogRepository.save(waLog);
    }

    // ── Webhook — delivery / read receipts ───────────────────────────

    public void handleWebhook(Map<String, Object> payload) {
        try {
            // Meta sends: entry[].changes[].value.statuses[]
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entry =
                    (List<Map<String, Object>>) payload.get("entry");
            if (entry == null) return;

            for (Map<String, Object> e : entry) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> changes =
                        (List<Map<String, Object>>) e.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> value =
                            (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> statuses =
                            (List<Map<String, Object>>) value.get("statuses");
                    if (statuses == null) continue;

                    for (Map<String, Object> status : statuses) {
                        String msgId = (String) status.get("id");
                        String st    = (String) status.get("status");
                        updateStatus(msgId, st);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Webhook parse error: {}", ex.getMessage());
        }
    }

    private void updateStatus(String waMessageId, String status) {
        waLogRepository.findByWaMessageId(waMessageId).ifPresent(log -> {
            switch (status) {
                case "delivered" -> { log.setStatus(WhatsAppLog.MessageStatus.DELIVERED);
                                      log.setDeliveredAt(LocalDateTime.now()); }
                case "read"      -> { log.setStatus(WhatsAppLog.MessageStatus.READ);
                                      log.setReadAt(LocalDateTime.now()); }
                case "failed"    -> log.setStatus(WhatsAppLog.MessageStatus.FAILED);
            }
            waLogRepository.save(log);
        });
    }

    // ── Templates CRUD ───────────────────────────────────────────────

    public List<WhatsAppTemplate> getMyTemplates(String email) {
        return waTemplateRepository.findByUser(getUser(email));
    }

    @Transactional
    public ApiResponse<WhatsAppTemplate> createTemplate(String name, String language,
                                                         String body, String category,
                                                         String email) {
        User user = getUser(email);
        if (waTemplateRepository.existsByUserAndName(user, name))
            return ApiResponse.error("Template name already exists");

        WhatsAppTemplate tpl = WhatsAppTemplate.builder()
                .user(user).name(name).language(language)
                .body(body).category(category)
                .status(WhatsAppTemplate.TemplateStatus.PENDING).build();
        return ApiResponse.success(waTemplateRepository.save(tpl), "Template submitted for approval");
    }

    // ── Logs & Reports ───────────────────────────────────────────────

    public Page<WhatsAppLog> getLogs(String email, int page, int size) {
        return waLogRepository.findByUserOrderByCreatedAtDesc(
                getUser(email), PageRequest.of(page, size));
    }

    public Page<WhatsAppLog> getLogsByStatus(String email,
                                              WhatsAppLog.MessageStatus status,
                                              int page, int size) {
        return waLogRepository.findByUserAndStatusOrderByCreatedAtDesc(
                getUser(email), status, PageRequest.of(page, size));
    }

    public Map<String, Object> getStats(String email) {
        User user = getUser(email);
        long sent      = waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.SENT)
                       + waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.DELIVERED)
                       + waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.READ);
        long delivered = waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.DELIVERED)
                       + waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.READ);
        long read      = waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.READ);
        long failed    = waLogRepository.countByUserAndStatus(user, WhatsAppLog.MessageStatus.FAILED);
        long today     = waLogRepository.countByUserAndCreatedAtAfter(
                user, LocalDateTime.now().toLocalDate().atStartOfDay());
        Double spent   = waLogRepository.sumCostByUser(user);
        Wallet wallet  = walletRepository.findByUser(user).orElseThrow();

        return Map.of(
                "totalSent", sent,
                "totalDelivered", delivered,
                "totalRead", read,
                "totalFailed", failed,
                "sentToday", today,
                "totalSpent", spent != null ? spent : 0.0,
                "walletBalance", wallet.getBalance(),
                "deliveryRate", sent > 0 ? Math.round(((double) delivered / sent) * 10000.0) / 100.0 : 0
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void checkAndDeduct(User user, double cost) {
        Wallet wallet = walletRepository.findByUser(user).orElseThrow();
        if (wallet.getBalance() < cost)
            throw new RuntimeException("Insufficient balance. Required: ₹" + cost
                    + ", Available: ₹" + wallet.getBalance());
        walletService.deductBalance(user, cost);
    }

    private String clean(String mobile) {
        return mobile.trim().replaceAll("[^0-9]", "");
    }

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null) return List.of();
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return List.of(); }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
