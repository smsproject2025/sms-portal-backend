package com.smsportal.repository;

import com.smsportal.model.IncomingWhatsAppMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomingWhatsAppMessageRepository
        extends JpaRepository<IncomingWhatsAppMessage, Long> {

    Page<IncomingWhatsAppMessage> findAllByOrderByReceivedAtDesc(Pageable pageable);

    Page<IncomingWhatsAppMessage> findByReadStatusOrderByReceivedAtDesc(
            IncomingWhatsAppMessage.ReadStatus status, Pageable pageable);

    long countByReadStatus(IncomingWhatsAppMessage.ReadStatus status);

    boolean existsByWaMessageId(String waMessageId);
}
