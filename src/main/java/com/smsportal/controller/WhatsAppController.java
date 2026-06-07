package com.smsportal.controller;

import com.smsportal.dto.ApiResponse;
import com.smsportal.model.*;
import com.smsportal.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final WhatsAppContactService contactService;
    private final WhatsAppCampaignService campaignService;
    private final WhatsAppInboxService inboxService;

    @Value("${whatsapp.webhook-verify-token}")
    private String verifyToken;

    // ── Send ─────────────────────────────────────────────────────────

    @PostMapping("/send/text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendText(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        @SuppressWarnings("unchecked")
        List<String> mobiles = (List<String>) body.get("mobiles");
        return ResponseEntity.ok(
                whatsAppService.sendText(mobiles, (String) body.get("message"), user.getUsername()));
    }

    @PostMapping("/send/template")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendTemplate(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        @SuppressWarnings("unchecked")
        List<String> mobiles = (List<String>) body.get("mobiles");
        @SuppressWarnings("unchecked")
        List<String> params = (List<String>) body.getOrDefault("params", List.of());
        return ResponseEntity.ok(whatsAppService.sendTemplate(
                mobiles,
                (String) body.get("templateName"),
                (String) body.getOrDefault("templateLanguage", "en_US"),
                params, user.getUsername()));
    }

    @PostMapping("/send/media")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMedia(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        @SuppressWarnings("unchecked")
        List<String> mobiles = (List<String>) body.get("mobiles");
        WhatsAppLog.MessageType mediaType =
                WhatsAppLog.MessageType.valueOf((String) body.getOrDefault("mediaType", "IMAGE"));
        return ResponseEntity.ok(whatsAppService.sendMedia(
                mobiles, (String) body.get("mediaUrl"), mediaType,
                (String) body.get("caption"), (String) body.get("filename"),
                user.getUsername()));
    }

    // ── Logs & Stats ─────────────────────────────────────────────────

    @GetMapping("/logs")
    public ResponseEntity<Page<WhatsAppLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(whatsAppService.getLogs(user.getUsername(), page, size));
    }

    @GetMapping("/logs/status/{status}")
    public ResponseEntity<Page<WhatsAppLog>> getLogsByStatus(
            @PathVariable WhatsAppLog.MessageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(
                whatsAppService.getLogsByStatus(user.getUsername(), status, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(whatsAppService.getStats(user.getUsername()));
    }

    // ── Templates ────────────────────────────────────────────────────

    @GetMapping("/templates")
    public ResponseEntity<List<WhatsAppTemplate>> getTemplates(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(whatsAppService.getMyTemplates(user.getUsername()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<WhatsAppTemplate>> createTemplate(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(whatsAppService.createTemplate(
                body.get("name"), body.get("language"),
                body.get("body"), body.get("category"),
                user.getUsername()));
    }

    // ── Contacts ─────────────────────────────────────────────────────

    @GetMapping("/contacts")
    public ResponseEntity<Page<WhatsAppContact>> getContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.getContacts(user.getUsername(), page, size));
    }

    @PostMapping("/contacts")
    public ResponseEntity<ApiResponse<WhatsAppContact>> addContact(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.addContact(
                body.get("mobile"), body.get("name"),
                body.get("email"), body.get("tags"),
                user.getUsername()));
    }

    @PostMapping("/contacts/import")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> importContacts(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.importFromCsv(file, user.getUsername()));
    }

    @PutMapping("/contacts/{id}/opt-out")
    public ResponseEntity<ApiResponse<String>> optOut(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.optOut(id, user.getUsername()));
    }

    @DeleteMapping("/contacts/{id}")
    public ResponseEntity<ApiResponse<String>> deleteContact(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.deleteContact(id, user.getUsername()));
    }

    @GetMapping("/contacts/stats")
    public ResponseEntity<Map<String, Long>> getContactStats(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(contactService.getContactStats(user.getUsername()));
    }

    // ── Campaigns ────────────────────────────────────────────────────

    @GetMapping("/campaigns")
    public ResponseEntity<Page<WhatsAppCampaign>> getCampaigns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(campaignService.getCampaigns(user.getUsername(), page, size));
    }

    @PostMapping("/campaigns")
    public ResponseEntity<ApiResponse<WhatsAppCampaign>> createCampaign(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(campaignService.createCampaign(body, user.getUsername()));
    }

    @PostMapping("/campaigns/{id}/launch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> launchCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(campaignService.launchCampaign(id, user.getUsername()));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCampaign(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(campaignService.deleteCampaign(id, user.getUsername()));
    }

    // ── Inbox (Incoming Messages) ─────────────────────────────────────

    @GetMapping("/inbox")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<IncomingWhatsAppMessage>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inboxService.getInbox(page, size));
    }

    @GetMapping("/inbox/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", inboxService.getUnreadCount()));
    }

    @PutMapping("/inbox/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        inboxService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/inbox/mark-all-read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAllRead() {
        inboxService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    // ── Meta Webhook ─────────────────────────────────────────────────

    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> payload) {
        // Route to delivery updates OR incoming messages
        whatsAppService.handleWebhook(payload);
        inboxService.processIncomingWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
