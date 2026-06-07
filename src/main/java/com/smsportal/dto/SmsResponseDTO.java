package com.smsportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsResponseDTO {

    private String status;
    private String message;
    private String batchId;
    private int totalNumbers;
    private double totalCost;
    private double remainingBalance;
    private List<Long> logIds;
}
