package com.smsportal.controller;

import com.smsportal.dto.ApiResponse;
import com.smsportal.model.SenderID;
import com.smsportal.model.User;
import com.smsportal.repository.SenderIDRepository;
import com.smsportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sender-ids")
@RequiredArgsConstructor
public class SenderIDController {

    private final SenderIDRepository senderIDRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<SenderID>> getMySenderIds(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(senderIDRepository.findByUser(user));
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<SenderID>> requestSenderId(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserByEmail(userDetails.getUsername());
        String senderId = body.get("senderId");

        if (senderIDRepository.existsByUserAndSenderId(user, senderId)) {
            return ResponseEntity.ok(ApiResponse.error("Sender ID already requested"));
        }

        SenderID newSenderId = SenderID.builder()
                .user(user)
                .senderId(senderId)
                .status(SenderID.SenderIdStatus.PENDING)
                .build();

        senderIDRepository.save(newSenderId);
        return ResponseEntity.ok(ApiResponse.success(newSenderId, "Sender ID request submitted"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
