package com.smsportal.service;

import com.smsportal.dto.ApiResponse;
import com.smsportal.model.User;
import com.smsportal.model.WhatsAppContact;
import com.smsportal.repository.UserRepository;
import com.smsportal.repository.WhatsAppContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppContactService {

    private final WhatsAppContactRepository contactRepository;
    private final UserRepository userRepository;

    public Page<WhatsAppContact> getContacts(String email, int page, int size) {
        return contactRepository.findByUserOrderByCreatedAtDesc(
                getUser(email), PageRequest.of(page, size));
    }

    @Transactional
    public ApiResponse<WhatsAppContact> addContact(String mobile, String name,
                                                    String email, String tags,
                                                    String userEmail) {
        User user = getUser(userEmail);
        String cleaned = cleanMobile(mobile);

        if (contactRepository.existsByUserAndMobile(user, cleaned)) {
            return ApiResponse.error("Contact with this number already exists");
        }

        WhatsAppContact contact = WhatsAppContact.builder()
                .user(user).mobile(cleaned).name(name)
                .email(email).tags(tags)
                .optInStatus(WhatsAppContact.OptInStatus.OPTED_IN)
                .optInAt(LocalDateTime.now())
                .build();

        return ApiResponse.success(contactRepository.save(contact), "Contact added");
    }

    @Transactional
    public ApiResponse<Map<String, Integer>> importFromCsv(MultipartFile file, String userEmail) {
        User user = getUser(userEmail);
        int added = 0, skipped = 0;

        try {
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                    .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

            for (CSVRecord record : parser) {
                String mobile = cleanMobile(record.get(0));
                String name   = safeGet(record, "name", "");
                String email  = safeGet(record, "email", "");
                String tags   = safeGet(record, "tags", "");

                if (mobile.length() < 10) { skipped++; continue; }
                if (contactRepository.existsByUserAndMobile(user, mobile)) { skipped++; continue; }

                WhatsAppContact c = WhatsAppContact.builder()
                        .user(user).mobile(mobile).name(name)
                        .email(email).tags(tags)
                        .optInStatus(WhatsAppContact.OptInStatus.OPTED_IN)
                        .optInAt(LocalDateTime.now())
                        .build();
                contactRepository.save(c);
                added++;
            }

            return ApiResponse.success(Map.of("added", added, "skipped", skipped),
                    added + " contacts imported");

        } catch (Exception e) {
            log.error("CSV import error: {}", e.getMessage());
            return ApiResponse.error("CSV parse failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<String> optOut(Long contactId, String userEmail) {
        User user = getUser(userEmail);
        WhatsAppContact contact = contactRepository.findById(contactId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contact.setOptInStatus(WhatsAppContact.OptInStatus.OPTED_OUT);
        contact.setOptOutAt(LocalDateTime.now());
        contactRepository.save(contact);
        return ApiResponse.success("Contact opted out", "OK");
    }

    @Transactional
    public ApiResponse<String> deleteContact(Long contactId, String userEmail) {
        User user = getUser(userEmail);
        WhatsAppContact contact = contactRepository.findById(contactId)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        contactRepository.delete(contact);
        return ApiResponse.success("Contact deleted", "OK");
    }

    public Map<String, Long> getContactStats(String userEmail) {
        User user = getUser(userEmail);
        return Map.of(
                "total",    contactRepository.countByUserAndOptInStatus(user, WhatsAppContact.OptInStatus.OPTED_IN)
                          + contactRepository.countByUserAndOptInStatus(user, WhatsAppContact.OptInStatus.OPTED_OUT),
                "optedIn",  contactRepository.countByUserAndOptInStatus(user, WhatsAppContact.OptInStatus.OPTED_IN),
                "optedOut", contactRepository.countByUserAndOptInStatus(user, WhatsAppContact.OptInStatus.OPTED_OUT)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String cleanMobile(String mobile) {
        mobile = mobile.trim().replaceAll("[^0-9+]", "");
        if (mobile.startsWith("+")) mobile = mobile.substring(1);
        if (mobile.length() == 10) mobile = "91" + mobile;
        return mobile;
    }

    private String safeGet(CSVRecord record, String key, String defaultVal) {
        try { return record.isMapped(key) ? record.get(key) : defaultVal; }
        catch (Exception e) { return defaultVal; }
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
