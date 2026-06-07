package com.smsportal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO {

    private Long walletId;
    private double balance;
    private double totalRecharge;
    private double totalSpent;

    // For recharge request
    @Min(value = 10, message = "Minimum recharge amount is ₹10")
    private double amount;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
