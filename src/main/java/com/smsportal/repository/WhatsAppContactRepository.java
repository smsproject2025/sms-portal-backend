package com.smsportal.repository;

import com.smsportal.model.User;
import com.smsportal.model.WhatsAppContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppContactRepository extends JpaRepository<WhatsAppContact, Long> {

    Page<WhatsAppContact> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<WhatsAppContact> findByUserAndMobile(User user, String mobile);

    List<WhatsAppContact> findByUserAndOptInStatus(User user, WhatsAppContact.OptInStatus status);

    @Query("SELECT c FROM WhatsAppContact c WHERE c.user = :user AND c.active = true " +
           "AND c.optInStatus = 'OPTED_IN' AND (:tag IS NULL OR c.tags LIKE %:tag%)")
    List<WhatsAppContact> findActiveOptedInByUserAndTag(@Param("user") User user,
                                                         @Param("tag") String tag);

    long countByUserAndOptInStatus(User user, WhatsAppContact.OptInStatus status);

    boolean existsByUserAndMobile(User user, String mobile);
}
