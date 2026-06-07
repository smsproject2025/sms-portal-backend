package com.smsportal.controller;

import com.smsportal.dto.ApiResponse;
import com.smsportal.dto.SmsRequestDTO;
import com.smsportal.dto.SmsResponseDTO;
import com.smsportal.model.SmsLog;
import com.smsportal.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<SmsResponseDTO>> sendSms(
            @Valid @RequestBody SmsRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smsService.sendSms(dto, userDetails.getUsername()));
    }

    @PostMapping("/send-bulk-csv")
    public ResponseEntity<ApiResponse<SmsResponseDTO>> sendBulkFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("senderId") String senderId,
            @RequestParam("message") String message,
            @RequestParam(value = "type", defaultValue = "PROMOTIONAL") SmsLog.SmsType type,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smsService.sendBulkFromCsv(file, senderId, message, type, userDetails.getUsername()));
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<SmsLog>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smsService.getReports(userDetails.getUsername(), page, size));
    }

    @GetMapping("/reports/status/{status}")
    public ResponseEntity<Page<SmsLog>> getReportsByStatus(
            @PathVariable SmsLog.SmsStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(smsService.getReportsByStatus(userDetails.getUsername(), status, page, size));
    }

    @PostMapping("/webhook/delivery")
    public ResponseEntity<Void> deliveryWebhook(@RequestBody Map<String, String> payload) {
        smsService.updateDeliveryStatus(payload);
        return ResponseEntity.ok().build();
    }
}
