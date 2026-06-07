package com.smsportal.repository;

import com.smsportal.model.User;
import com.smsportal.model.WhatsAppTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppTemplateRepository extends JpaRepository<WhatsAppTemplate, Long> {

    List<WhatsAppTemplate> findByUser(User user);

    List<WhatsAppTemplate> findByUserAndStatus(User user, WhatsAppTemplate.TemplateStatus status);

    Page<WhatsAppTemplate> findByStatus(WhatsAppTemplate.TemplateStatus status, Pageable pageable);

    boolean existsByUserAndName(User user, String name);
}
