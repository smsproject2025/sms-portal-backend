package com.smsportal.controller;

import com.smsportal.dto.ApiResponse;
import com.smsportal.model.SenderID;
import com.smsportal.model.User;
import com.smsportal.repository.SenderIDRepository;
import com.smsportal.repository.SmsLogRepository;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WhatsAppLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final SmsLogRepository smsLogRepository;
    private final SenderIDRepository senderIDRepository;
    private final WhatsAppLogRepository waLogRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",   userRepository.countAllUsers());
        stats.put("activeUsers",  userRepository.countActiveUsers());
        stats.put("totalSmsSent", smsLogRepository.countAllSentAfter(LocalDateTime.now().minusYears(10)));
        stats.put("smsSentToday", smsLogRepository.countAllSentAfter(LocalDateTime.now().toLocalDate().atStartOfDay()));
        stats.put("totalWaSent",  waLogRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userRepository.findAll(PageRequest.of(page, size)));
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<String>> toggleUserActive(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(
                user.isActive() ? "User activated" : "User deactivated",
                "Success"
        ));
    }

    @GetMapping("/sender-ids/pending")
    public ResponseEntity<Page<SenderID>> getPendingSenderIds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                senderIDRepository.findByStatus(SenderID.SenderIdStatus.PENDING, PageRequest.of(page, size))
        );
    }

    @PutMapping("/sender-ids/{id}/approve")
    public ResponseEntity<ApiResponse<String>> approveSenderId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminDetails) {
        SenderID senderId = senderIDRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SenderID not found"));

        User admin = userRepository.findByEmail(adminDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        senderId.setStatus(SenderID.SenderIdStatus.APPROVED);
        senderId.setApprovedAt(LocalDateTime.now());
        senderId.setApprovedBy(admin);
        senderIDRepository.save(senderId);

        return ResponseEntity.ok(ApiResponse.success("Sender ID approved", "Success"));
    }

    @PutMapping("/sender-ids/{id}/reject")
    public ResponseEntity<ApiResponse<String>> rejectSenderId(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        SenderID senderId = senderIDRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SenderID not found"));

        senderId.setStatus(SenderID.SenderIdStatus.REJECTED);
        senderId.setReason(body.get("reason"));
        senderIDRepository.save(senderId);

        return ResponseEntity.ok(ApiResponse.success("Sender ID rejected", "Success"));
    }

    @GetMapping("/sms-logs")
    public ResponseEntity<?> getAllSmsLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(smsLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }
}
