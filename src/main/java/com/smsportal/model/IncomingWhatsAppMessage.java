package com.smsportal.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_incoming")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomingWhatsAppMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromMobile;       // sender's number (E.164)
    private String fromName;         // display name if available

    @Column(columnDefinition = "TEXT")
    private String body;             // text content

    private String waMessageId;      // Meta's message ID
    private String messageType;      // text, image, document, audio, video, sticker

    private String mediaUrl;         // for media messages
    private String mediaCaption;
    private String mediaMimeType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReadStatus readStatus = ReadStatus.UNREAD;

    @Column(columnDefinition = "JSON")
    private String rawPayload;       // full JSON from Meta webhook

    @CreationTimestamp
    private LocalDateTime receivedAt;

    private LocalDateTime readAt;

    public enum ReadStatus {
        UNREAD, READ
    }
}
