package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String name;           // e.g. "order_confirmation"

    @Column(nullable = false)
    private String language;       // e.g. "en_US"

    @Column(columnDefinition = "TEXT")
    private String body;           // preview with {{1}}, {{2}} placeholders

    private String category;       // MARKETING, UTILITY, AUTHENTICATION

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TemplateStatus status = TemplateStatus.PENDING;

    private String waTemplateId;   // ID returned by WhatsApp after approval

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum TemplateStatus {
        PENDING, APPROVED, REJECTED
    }
}
