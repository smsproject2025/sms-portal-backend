package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sender_ids")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SenderID {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String senderId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SenderIdStatus status = SenderIdStatus.PENDING;

    private String reason;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User approvedBy;

    public enum SenderIdStatus {
        PENDING, APPROVED, REJECTED
    }
}
