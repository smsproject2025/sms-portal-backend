package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_contacts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "mobile"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String mobile;         // E.164 format: 919876543210

    private String name;
    private String email;
    private String tags;           // comma-separated tags for segmentation

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private OptInStatus optInStatus = OptInStatus.OPTED_IN;

    private LocalDateTime optInAt;
    private LocalDateTime optOutAt;

    @Builder.Default
    private boolean active = true;

    private String lastMessageId;
    private LocalDateTime lastMessageAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum OptInStatus {
        OPTED_IN, OPTED_OUT, PENDING
    }
}
