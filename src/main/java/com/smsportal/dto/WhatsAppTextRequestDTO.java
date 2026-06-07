package com.smsportal.dto;

import com.smsportal.model.WhatsAppLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ─── Outbound Request DTOs ───────────────────────────────────────────

/** Send a plain text WhatsApp message */
@Data
public class WhatsAppTextRequestDTO {
    @NotEmpty(message = "At least one mobile number is required")
    private List<String> mobiles;

    @NotBlank(message = "Message is required")
    private String message;
}

/** Send a pre-approved template message */
@Data
class WhatsAppTemplateRequestDTO {
    @NotEmpty
    private List<String> mobiles;

    @NotBlank
    private String templateName;

    @NotBlank
    private String templateLanguage;

    /** Ordered list of values for {{1}}, {{2}} … placeholders */
    private List<String> params;
}

/** Send a media message (image / document / video / audio) */
@Data
class WhatsAppMediaRequestDTO {
    @NotEmpty
    private List<String> mobiles;

    @NotBlank
    private String mediaUrl;

    private WhatsAppLog.MessageType mediaType; // IMAGE, DOCUMENT, VIDEO, AUDIO
    private String caption;
    private String filename; // required for DOCUMENT
}

// ─── Response DTO ────────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WhatsAppResponseDTO {
    private String status;
    private String message;
    private String batchId;
    private int totalNumbers;
    private double totalCost;
    private double remainingBalance;
    private List<Long> logIds;
}

// ─── Template management ─────────────────────────────────────────────

@Data
class WhatsAppTemplateCreateDTO {
    @NotBlank private String name;
    @NotBlank private String language;
    @NotBlank private String body;
    private String category; // MARKETING | UTILITY | AUTHENTICATION
}

// ─── Dashboard Stats ─────────────────────────────────────────────────

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WhatsAppStatsDTO {
    private long totalSent;
    private long totalDelivered;
    private long totalRead;
    private long totalFailed;
    private double walletBalance;
    private double totalSpent;
    private long sentToday;
    private double deliveryRate;
}
