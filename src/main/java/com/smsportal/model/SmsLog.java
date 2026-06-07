package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String senderId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SmsType type = SmsType.PROMOTIONAL;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SmsStatus status = SmsStatus.QUEUED;

    private double cost;

    private String gatewayMessageId;

    private String gatewayResponse;

    private String errorMessage;

    @Column(name = "batch_id")
    private String batchId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    public enum SmsType {
        PROMOTIONAL, TRANSACTIONAL, OTP
    }

    public enum SmsStatus {
        QUEUED, SENT, DELIVERED, FAILED, REJECTED
    }
}
