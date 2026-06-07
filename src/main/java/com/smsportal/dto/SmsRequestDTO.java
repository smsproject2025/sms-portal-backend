package com.smsportal.dto;

import com.smsportal.model.SmsLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SmsRequestDTO {

    @NotEmpty(message = "At least one mobile number is required")
    private List<String> mobiles;

    @NotBlank(message = "Message is required")
    private String message;

    @NotBlank(message = "Sender ID is required")
    private String senderId;

    private SmsLog.SmsType type = SmsLog.SmsType.PROMOTIONAL;

    private String scheduleAt; // ISO datetime string, null for immediate
}
