package com.smsportal.service;

import com.smsportal.config.RabbitMQConfig;
import com.smsportal.dto.ApiResponse;
import com.smsportal.dto.SmsRequestDTO;
import com.smsportal.dto.SmsResponseDTO;
import com.smsportal.model.SmsLog;
import com.smsportal.model.User;
import com.smsportal.repository.SmsLogRepository;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final SmsLogRepository smsLogRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SmsGatewayService gatewayService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.sms-rate-promotional}")
    private double ratePromotional;

    @Value("${app.sms-rate-transactional}")
    private double rateTransactional;

    @Value("${app.sms-rate-otp}")
    private double rateOtp;

    @Transactional
    public ApiResponse<SmsResponseDTO> sendSms(SmsRequestDTO dto, String email) {
        User user = getUserByEmail(email);

        double costPerSms = getCostPerSms(dto.getType());
        double totalCost = costPerSms * dto.getMobiles().size();

        // Check balance
        var wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getBalance() < totalCost) {
            return ApiResponse.error("Insufficient balance. Required: ₹" + totalCost + ", Available: ₹" + wallet.getBalance());
        }

        // Deduct balance
        walletService.deductBalance(user, totalCost);

        // Create SMS logs and queue
        String batchId = UUID.randomUUID().toString();
        List<Long> logIds = new ArrayList<>();

        for (String mobile : dto.getMobiles()) {
            String cleanMobile = mobile.trim().replaceAll("[^0-9]", "");
            if (cleanMobile.length() < 10) continue;
            if (cleanMobile.length() == 12 && cleanMobile.startsWith("91")) {
                cleanMobile = cleanMobile.substring(2);
            }

            SmsLog smsLog = SmsLog.builder()
                    .user(user)
                    .mobile(cleanMobile)
                    .message(dto.getMessage())
                    .senderId(dto.getSenderId())
                    .type(dto.getType())
                    .status(SmsLog.SmsStatus.QUEUED)
                    .cost(costPerSms)
                    .batchId(batchId)
                    .build();

            smsLog = smsLogRepository.save(smsLog);
            logIds.add(smsLog.getId());

            // Push to queue
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SMS_EXCHANGE,
                    RabbitMQConfig.SMS_ROUTING_KEY,
                    smsLog.getId()
            );
        }

        return ApiResponse.success(
                SmsResponseDTO.builder()
                        .status("queued")
                        .message("SMS queued successfully")
                        .batchId(batchId)
                        .totalNumbers(logIds.size())
                        .totalCost(totalCost)
                        .remainingBalance(wallet.getBalance() - totalCost)
                        .logIds(logIds)
                        .build(),
                "SMS queued"
        );
    }

    @Transactional
    public ApiResponse<SmsResponseDTO> sendBulkFromCsv(MultipartFile file, String senderId,
                                                         String message, SmsLog.SmsType type, String email) {
        try {
            List<String> mobiles = new ArrayList<>();
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                    .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

            for (CSVRecord record : parser) {
                String mobile = record.get(0).trim();
                if (!mobile.isEmpty()) {
                    mobiles.add(mobile);
                }
            }

            SmsRequestDTO dto = new SmsRequestDTO();
            dto.setMobiles(mobiles);
            dto.setMessage(message);
            dto.setSenderId(senderId);
            dto.setType(type);

            return sendSms(dto, email);

        } catch (Exception e) {
            log.error("CSV parse error: {}", e.getMessage());
            return ApiResponse.error("Failed to parse CSV: " + e.getMessage());
        }
    }

    // RabbitMQ Consumer
    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void processSmsFromQueue(Long logId) {
        SmsLog smsLog = smsLogRepository.findById(logId).orElse(null);
        if (smsLog == null) {
            log.warn("SMS log not found for id: {}", logId);
            return;
        }

        try {
            String msgId = gatewayService.sendSms(smsLog.getMobile(), smsLog.getMessage(), smsLog.getSenderId());
            smsLog.setStatus(SmsLog.SmsStatus.SENT);
            smsLog.setGatewayMessageId(msgId);
            smsLog.setSentAt(LocalDateTime.now());
            log.info("SMS sent successfully to {} | MsgId: {}", smsLog.getMobile(), msgId);
        } catch (Exception e) {
            smsLog.setStatus(SmsLog.SmsStatus.FAILED);
            smsLog.setErrorMessage(e.getMessage());
            log.error("SMS send failed for {}: {}", smsLog.getMobile(), e.getMessage());
        }

        smsLogRepository.save(smsLog);
    }

    public void updateDeliveryStatus(Map<String, String> payload) {
        String gatewayMsgId = payload.get("request_id");
        String status = payload.get("status");

        if (gatewayMsgId == null) return;

        smsLogRepository.findByGatewayMessageId(gatewayMsgId).ifPresent(log -> {
            if ("delivered".equalsIgnoreCase(status)) {
                log.setStatus(SmsLog.SmsStatus.DELIVERED);
                log.setDeliveredAt(LocalDateTime.now());
            } else if ("failed".equalsIgnoreCase(status) || "rejected".equalsIgnoreCase(status)) {
                log.setStatus(SmsLog.SmsStatus.FAILED);
            }
            smsLogRepository.save(log);
        });
    }

    public Page<SmsLog> getReports(String email, int page, int size) {
        User user = getUserByEmail(email);
        return smsLogRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
    }

    public Page<SmsLog> getReportsByStatus(String email, SmsLog.SmsStatus status, int page, int size) {
        User user = getUserByEmail(email);
        return smsLogRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, PageRequest.of(page, size));
    }

    private double getCostPerSms(SmsLog.SmsType type) {
        return switch (type) {
            case TRANSACTIONAL -> rateTransactional;
            case OTP -> rateOtp;
            default -> ratePromotional;
        };
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
