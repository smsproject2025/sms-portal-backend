package com.smsportal.service;

import com.smsportal.dto.DashboardStatsDTO;
import com.smsportal.model.SmsLog;
import com.smsportal.model.User;
import com.smsportal.model.Wallet;
import com.smsportal.repository.SmsLogRepository;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final SmsLogRepository smsLogRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public DashboardStatsDTO getDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wallet wallet = walletRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        long totalSent = smsLogRepository.countByUserAndStatus(user, SmsLog.SmsStatus.SENT)
                + smsLogRepository.countByUserAndStatus(user, SmsLog.SmsStatus.DELIVERED);
        long totalDelivered = smsLogRepository.countByUserAndStatus(user, SmsLog.SmsStatus.DELIVERED);
        long totalFailed = smsLogRepository.countByUserAndStatus(user, SmsLog.SmsStatus.FAILED);
        long totalPending = smsLogRepository.countByUserAndStatus(user, SmsLog.SmsStatus.QUEUED);
        long sentToday = smsLogRepository.countByUserAndCreatedAtAfter(user, LocalDateTime.now().toLocalDate().atStartOfDay());

        Double totalSpent = smsLogRepository.sumCostByUser(user);
        double deliveryRate = totalSent > 0 ? ((double) totalDelivered / totalSent) * 100 : 0;

        // Last 7 days stats
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Object[]> rawStats = smsLogRepository.getDailyStats(user, sevenDaysAgo);

        List<DashboardStatsDTO.DailyStatDTO> dailyStats = new ArrayList<>();
        for (Object[] row : rawStats) {
            dailyStats.add(DashboardStatsDTO.DailyStatDTO.builder()
                    .date(row[0].toString())
                    .total(((Number) row[1]).longValue())
                    .delivered(((Number) row[2]).longValue())
                    .build());
        }

        return DashboardStatsDTO.builder()
                .totalSent(totalSent)
                .totalDelivered(totalDelivered)
                .totalFailed(totalFailed)
                .totalPending(totalPending)
                .walletBalance(wallet.getBalance())
                .totalSpent(totalSpent != null ? totalSpent : 0.0)
                .sentToday(sentToday)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .dailyStats(dailyStats)
                .build();
    }
}
