package com.smsportal.repository;

import com.smsportal.model.User;
import com.smsportal.model.WhatsAppCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WhatsAppCampaignRepository extends JpaRepository<WhatsAppCampaign, Long> {

    Page<WhatsAppCampaign> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<WhatsAppCampaign> findByStatusAndScheduledAtBefore(
            WhatsAppCampaign.CampaignStatus status, LocalDateTime time);
}
