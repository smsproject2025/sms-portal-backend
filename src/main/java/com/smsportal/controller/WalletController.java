package com.smsportal.controller;

import com.smsportal.dto.ApiResponse;
import com.smsportal.dto.WalletDTO;
import com.smsportal.model.Transaction;
import com.smsportal.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletDTO> getWallet(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getWallet(userDetails.getUsername()));
    }

    @PostMapping("/recharge/create-order")
    public ResponseEntity<ApiResponse<Map<String, String>>> createOrder(
            @RequestBody Map<String, Double> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        double amount = body.get("amount");
        return ResponseEntity.ok(walletService.createRechargeOrder(amount, userDetails.getUsername()));
    }

    @PostMapping("/recharge/verify")
    public ResponseEntity<ApiResponse<WalletDTO>> verifyPayment(
            @RequestBody WalletDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.verifyAndAddBalance(dto, userDetails.getUsername()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getTransactionHistory(userDetails.getUsername(), page, size));
    }
}
