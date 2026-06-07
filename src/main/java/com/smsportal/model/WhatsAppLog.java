package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(nullable = false)
    private String mobile;         // recipient WhatsApp number (with country code)

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String message;        // for text messages

    private String templateName;   // for template messages
    private String templateLanguage;

    @Column(columnDefinition = "JSON")
    private String templateParams; // JSON array of template parameter values

    private String mediaUrl;       // for media messages (image/doc/video)
    private String mediaCaption;
    private String mediaFilename;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageStatus status = MessageStatus.QUEUED;

    private String waMessageId;    // WhatsApp Business API message ID
    private String errorCode;
    private String errorMessage;

    private double cost;
    private String batchId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;

    public enum MessageType {
        TEXT, TEMPLATE, IMAGE, DOCUMENT, VIDEO, AUDIO
    }

    public enum MessageStatus {
        QUEUED, SENT, DELIVERED, READ, FAILED, REJECTED
    }
}
