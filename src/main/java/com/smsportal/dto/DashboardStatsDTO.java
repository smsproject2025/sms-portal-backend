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
public class DashboardStatsDTO {

    private long totalSent;
    private long totalDelivered;
    private long totalFailed;
    private long totalPending;
    private double walletBalance;
    private double totalSpent;
    private long sentToday;
    private double deliveryRate;
    private List<DailyStatDTO> dailyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatDTO {
        private String date;
        private long total;
        private long delivered;
    }
}
