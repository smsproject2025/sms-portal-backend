package com.smsportal.controller;

import com.smsportal.dto.DashboardStatsDTO;
import com.smsportal.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(reportService.getDashboardStats(userDetails.getUsername()));
    }
}
