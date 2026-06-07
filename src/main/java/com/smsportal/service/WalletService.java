package com.smsportal.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smsportal.dto.ApiResponse;
import com.smsportal.dto.WalletDTO;
import com.smsportal.model.Transaction;
import com.smsportal.model.User;
import com.smsportal.model.Wallet;
import com.smsportal.repository.TransactionRepository;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key-id:not-configured}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret:not-configured}")
    private String razorpayKeySecret;

    public WalletDTO getWallet(String email) {
        User user = getUserByEmail(email);
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return WalletDTO.builder()
                .walletId(wallet.getId())
                .balance(wallet.getBalance())
                .totalRecharge(wallet.getTotalRecharge())
                .totalSpent(wallet.getTotalSpent())
                .build();
    }

    public ApiResponse<Map<String, String>> createRechargeOrder(double amount, String email) {
        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject options = new JSONObject();
            options.put("amount", (int) (amount * 100)); // convert to paise
            options.put("currency", "INR");
            options.put("receipt", "rcpt_" + System.currentTimeMillis());
            options.put("notes", new JSONObject().put("email", email));

            Order order = client.orders.create(options);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", String.valueOf(amount));
            response.put("currency", "INR");
            response.put("keyId", razorpayKeyId);

            return ApiResponse.success(response, "Order created");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            return ApiResponse.error("Payment gateway error: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<WalletDTO> verifyAndAddBalance(WalletDTO dto, String email) {
        // Verify Razorpay signature
        try {
            String payload = dto.getRazorpayOrderId() + "|" + dto.getRazorpayPaymentId();
            String generatedSignature = hmacSha256(payload, razorpayKeySecret);

            if (!generatedSignature.equals(dto.getRazorpaySignature())) {
                return ApiResponse.error("Payment verification failed: Invalid signature");
            }

            if (transactionRepository.existsByReferenceId(dto.getRazorpayPaymentId())) {
                return ApiResponse.error("Payment already processed");
            }

            User user = getUserByEmail(email);
            Wallet wallet = walletRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            double balanceBefore = wallet.getBalance();
            wallet.setBalance(balanceBefore + dto.getAmount());
            wallet.setTotalRecharge(wallet.getTotalRecharge() + dto.getAmount());
            walletRepository.save(wallet);

            Transaction txn = Transaction.builder()
                    .wallet(wallet)
                    .type(Transaction.TransactionType.CREDIT)
                    .amount(dto.getAmount())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(wallet.getBalance())
                    .description("Wallet recharge via Razorpay")
                    .referenceId(dto.getRazorpayPaymentId())
                    .status(Transaction.TransactionStatus.SUCCESS)
                    .build();
            transactionRepository.save(txn);

            log.info("Wallet recharged for user {} by ₹{}", email, dto.getAmount());

            return ApiResponse.success(
                    WalletDTO.builder()
                            .balance(wallet.getBalance())
                            .totalRecharge(wallet.getTotalRecharge())
                            .totalSpent(wallet.getTotalSpent())
                            .build(),
                    "Wallet recharged successfully"
            );

        } catch (Exception e) {
            log.error("Payment verification error: {}", e.getMessage());
            return ApiResponse.error("Payment verification failed");
        }
    }

    @Transactional
    public void deductBalance(User user, double amount) {
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        double balanceBefore = wallet.getBalance();
        wallet.setBalance(balanceBefore - amount);
        wallet.setTotalSpent(wallet.getTotalSpent() + amount);
        walletRepository.save(wallet);

        Transaction txn = Transaction.builder()
                .wallet(wallet)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .description("SMS charges")
                .build();
        transactionRepository.save(txn);
    }

    public Page<Transaction> getTransactionHistory(String email, int page, int size) {
        User user = getUserByEmail(email);
        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, PageRequest.of(page, size));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
