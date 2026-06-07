package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private WhatsAppLog.MessageType messageType;

    // For text campaigns
    @Column(columnDefinition = "TEXT")
    private String message;

    // For template campaigns
    private String templateName;
    private String templateLanguage;
    @Column(columnDefinition = "JSON")
    private String templateParams;

    // For media campaigns
    private String mediaUrl;
    private String mediaCaption;
    private String mediaFilename;

    // Targeting
    private String targetTags;      // comma-separated tags to target
    private int totalTargeted;
    private int totalSent;
    private int totalDelivered;
    private int totalRead;
    private int totalFailed;

    private double totalCost;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum CampaignStatus {
        DRAFT, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
