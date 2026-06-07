package com.smsportal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smsportal.dto.ApiResponse;
import com.smsportal.model.*;
import com.smsportal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCampaignService {

    private final WhatsAppCampaignRepository campaignRepository;
    private final WhatsAppContactRepository contactRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper;

    // ── Create Campaign ──────────────────────────────────────────────

    @Transactional
    public ApiResponse<WhatsAppCampaign> createCampaign(Map<String, Object> dto, String email) {
        User user = getUser(email);

        WhatsAppLog.MessageType msgType =
                WhatsAppLog.MessageType.valueOf((String) dto.getOrDefault("messageType", "TEXT"));

        WhatsAppCampaign.WhatsAppCampaignBuilder builder = WhatsAppCampaign.builder()
                .user(user)
                .name((String) dto.get("name"))
                .description((String) dto.getOrDefault("description", ""))
                .messageType(msgType)
                .targetTags((String) dto.getOrDefault("targetTags", ""))
                .status(WhatsAppCampaign.CampaignStatus.DRAFT);

        switch (msgType) {
            case TEXT -> builder.message((String) dto.get("message"));
            case TEMPLATE -> {
                builder.templateName((String) dto.get("templateName"));
                builder.templateLanguage((String) dto.getOrDefault("templateLanguage", "en_US"));
                @SuppressWarnings("unchecked")
                List<String> params = (List<String>) dto.getOrDefault("params", List.of());
                builder.templateParams(toJson(params));
            }
            default -> {
                builder.mediaUrl((String) dto.get("mediaUrl"));
                builder.mediaCaption((String) dto.getOrDefault("caption", ""));
                builder.mediaFilename((String) dto.getOrDefault("filename", ""));
            }
        }

        // Schedule time
        String scheduledAt = (String) dto.get("scheduledAt");
        if (scheduledAt != null && !scheduledAt.isBlank()) {
            builder.scheduledAt(LocalDateTime.parse(scheduledAt));
            builder.status(WhatsAppCampaign.CampaignStatus.SCHEDULED);
        }

        WhatsAppCampaign campaign = campaignRepository.save(builder.build());
        return ApiResponse.success(campaign, "Campaign created");
    }

    // ── Launch Campaign Now ──────────────────────────────────────────

    @Transactional
    public ApiResponse<Map<String, Object>> launchCampaign(Long campaignId, String email) {
        User user = getUser(email);
        WhatsAppCampaign campaign = campaignRepository.findById(campaignId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        if (campaign.getStatus() == WhatsAppCampaign.CampaignStatus.RUNNING ||
            campaign.getStatus() == WhatsAppCampaign.CampaignStatus.COMPLETED) {
            return ApiResponse.error("Campaign already " + campaign.getStatus().name().toLowerCase());
        }

        // Find target contacts
        String tag = campaign.getTargetTags();
        List<WhatsAppContact> contacts =
                contactRepository.findActiveOptedInByUserAndTag(user, tag.isBlank() ? null : tag);

        if (contacts.isEmpty())
            return ApiResponse.error("No opted-in contacts found for this campaign");

        List<String> mobiles = contacts.stream().map(WhatsAppContact::getMobile).toList();

        // Check balance
        double rate = switch (campaign.getMessageType()) {
            case TEMPLATE -> 0.55;
            case IMAGE, DOCUMENT, VIDEO, AUDIO -> 0.60;
            default -> 0.40;
        };
        double totalCost = rate * mobiles.size();
        Wallet wallet = walletRepository.findByUser(user).orElseThrow();
        if (wallet.getBalance() < totalCost)
            return ApiResponse.error("Insufficient balance. Required ₹" + totalCost);

        // Update campaign status
        campaign.setStatus(WhatsAppCampaign.CampaignStatus.RUNNING);
        campaign.setStartedAt(LocalDateTime.now());
        campaign.setTotalTargeted(mobiles.size());
        campaign.setTotalCost(totalCost);
        campaignRepository.save(campaign);

        // Send via WhatsApp service
        ApiResponse<Map<String, Object>> result = switch (campaign.getMessageType()) {
            case TEMPLATE -> {
                List<String> params = fromJson(campaign.getTemplateParams());
                yield whatsAppService.sendTemplate(mobiles, campaign.getTemplateName(),
                        campaign.getTemplateLanguage(), params, email);
            }
            case IMAGE, VIDEO -> whatsAppService.sendMedia(mobiles, campaign.getMediaUrl(),
                    campaign.getMessageType(), campaign.getMediaCaption(), null, email);
            case DOCUMENT -> whatsAppService.sendMedia(mobiles, campaign.getMediaUrl(),
                    WhatsAppLog.MessageType.DOCUMENT,
                    campaign.getMediaCaption(), campaign.getMediaFilename(), email);
            default -> whatsAppService.sendText(mobiles, campaign.getMessage(), email);
        };

        if (result.isSuccess()) {
            campaign.setStatus(WhatsAppCampaign.CampaignStatus.COMPLETED);
            campaign.setCompletedAt(LocalDateTime.now());
            campaign.setTotalSent(mobiles.size());
        } else {
            campaign.setStatus(WhatsAppCampaign.CampaignStatus.FAILED);
        }
        campaignRepository.save(campaign);

        return result;
    }

    // ── Scheduled Campaigns ──────────────────────────────────────────

    @Scheduled(fixedDelay = 60_000) // every minute
    public void processScheduledCampaigns() {
        List<WhatsAppCampaign> due = campaignRepository.findByStatusAndScheduledAtBefore(
                WhatsAppCampaign.CampaignStatus.SCHEDULED, LocalDateTime.now());

        for (WhatsAppCampaign c : due) {
            try {
                log.info("Auto-launching scheduled campaign #{}: {}", c.getId(), c.getName());
                launchCampaign(c.getId(), c.getUser().getEmail());
            } catch (Exception e) {
                log.error("Scheduled campaign #{} failed: {}", c.getId(), e.getMessage());
                c.setStatus(WhatsAppCampaign.CampaignStatus.FAILED);
                campaignRepository.save(c);
            }
        }
    }

    // ── Queries ──────────────────────────────────────────────────────

    public Page<WhatsAppCampaign> getCampaigns(String email, int page, int size) {
        return campaignRepository.findByUserOrderByCreatedAtDesc(
                getUser(email), PageRequest.of(page, size));
    }

    @Transactional
    public ApiResponse<String> deleteCampaign(Long id, String email) {
        User user = getUser(email);
        WhatsAppCampaign c = campaignRepository.findById(id)
                .filter(camp -> camp.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (c.getStatus() == WhatsAppCampaign.CampaignStatus.RUNNING)
            return ApiResponse.error("Cannot delete a running campaign");
        campaignRepository.delete(c);
        return ApiResponse.success("Campaign deleted", "OK");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (JsonProcessingException e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null) return List.of();
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return List.of(); }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
