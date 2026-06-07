package com.smsportal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smsportal.model.IncomingWhatsAppMessage;
import com.smsportal.model.WhatsAppContact;
import com.smsportal.repository.IncomingWhatsAppMessageRepository;
import com.smsportal.repository.WhatsAppContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppInboxService {

    private final IncomingWhatsAppMessageRepository inboxRepository;
    private final WhatsAppContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    /**
     * Parse the Meta Cloud API webhook payload for incoming messages
     * and store them in the inbox.
     */
    @Transactional
    public void processIncomingWebhook(Map<String, Object> payload) {
        try {
            String raw = objectMapper.writeValueAsString(payload);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) payload.get("entry");
            if (entries == null) return;

            for (Map<String, Object> entry : entries) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> changes =
                        (List<Map<String, Object>>) entry.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> value =
                            (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages =
                            (List<Map<String, Object>>) value.get("messages");
                    if (messages == null) continue;

                    // Contacts info (display name)
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> contacts =
                            (List<Map<String, Object>>) value.getOrDefault("contacts", List.of());
                    Map<String, String> nameMap = extractContactNames(contacts);

                    for (Map<String, Object> msg : messages) {
                        storeIncomingMessage(msg, nameMap, raw);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing incoming WA webhook: {}", e.getMessage());
        }
    }

    @Transactional
    public void storeIncomingMessage(Map<String, Object> msg,
                                      Map<String, String> nameMap,
                                      String rawPayload) {
        String waId = (String) msg.get("id");
        if (waId == null || inboxRepository.existsByWaMessageId(waId)) return;

        String from = (String) msg.get("from");
        String type = (String) msg.getOrDefault("type", "text");
        String displayName = nameMap.getOrDefault(from, "");

        IncomingWhatsAppMessage.IncomingWhatsAppMessageBuilder builder =
                IncomingWhatsAppMessage.builder()
                        .fromMobile(from)
                        .fromName(displayName)
                        .waMessageId(waId)
                        .messageType(type)
                        .rawPayload(rawPayload);

        // Extract content based on type
        switch (type) {
            case "text" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> textObj = (Map<String, Object>) msg.get("text");
                if (textObj != null) builder.body((String) textObj.get("body"));
            }
            case "image", "video", "audio", "document" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> media = (Map<String, Object>) msg.get(type);
                if (media != null) {
                    builder.mediaUrl((String) media.getOrDefault("link", ""))
                           .mediaCaption((String) media.getOrDefault("caption", ""))
                           .mediaMimeType((String) media.getOrDefault("mime_type", ""));
                }
            }
            case "button" -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> button = (Map<String, Object>) msg.get("button");
                if (button != null) builder.body("Button: " + button.get("text"));
            }
        }

        IncomingWhatsAppMessage saved = inboxRepository.save(builder.build());
        log.info("Stored incoming WA message from {} (type={})", from, type);

        // Auto-update contact's last message timestamp
        contactRepository.findAll().stream()
                .filter(c -> c.getMobile().equals(from))
                .findFirst()
                .ifPresent(contact -> {
                    contact.setLastMessageId(waId);
                    contact.setLastMessageAt(LocalDateTime.now());
                    contactRepository.save(contact);
                });
    }

    public Page<IncomingWhatsAppMessage> getInbox(int page, int size) {
        return inboxRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(page, size));
    }

    public Page<IncomingWhatsAppMessage> getUnread(int page, int size) {
        return inboxRepository.findByReadStatusOrderByReceivedAtDesc(
                IncomingWhatsAppMessage.ReadStatus.UNREAD, PageRequest.of(page, size));
    }

    public long getUnreadCount() {
        return inboxRepository.countByReadStatus(IncomingWhatsAppMessage.ReadStatus.UNREAD);
    }

    @Transactional
    public void markAsRead(Long id) {
        inboxRepository.findById(id).ifPresent(msg -> {
            msg.setReadStatus(IncomingWhatsAppMessage.ReadStatus.READ);
            msg.setReadAt(LocalDateTime.now());
            inboxRepository.save(msg);
        });
    }

    @Transactional
    public void markAllAsRead() {
        inboxRepository.findByReadStatusOrderByReceivedAtDesc(
                IncomingWhatsAppMessage.ReadStatus.UNREAD, PageRequest.of(0, Integer.MAX_VALUE))
                .forEach(msg -> {
                    msg.setReadStatus(IncomingWhatsAppMessage.ReadStatus.READ);
                    msg.setReadAt(LocalDateTime.now());
                    inboxRepository.save(msg);
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractContactNames(List<Map<String, Object>> contacts) {
        Map<String, String> result = new java.util.HashMap<>();
        for (Map<String, Object> c : contacts) {
            String waId = (String) c.get("wa_id");
            Map<String, Object> profile = (Map<String, Object>) c.getOrDefault("profile", Map.of());
            String name = (String) profile.getOrDefault("name", "");
            if (waId != null) result.put(waId, name);
        }
        return result;
    }
}
