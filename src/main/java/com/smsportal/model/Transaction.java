package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private double amount;

    private double balanceBefore;

    private double balanceAfter;

    private String description;

    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.SUCCESS;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TransactionType {
        CREDIT, DEBIT
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED
    }
}
