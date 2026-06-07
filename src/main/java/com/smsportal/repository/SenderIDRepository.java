package com.smsportal.repository;

import com.smsportal.model.SenderID;
import com.smsportal.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SenderIDRepository extends JpaRepository<SenderID, Long> {

    List<SenderID> findByUserAndStatus(User user, SenderID.SenderIdStatus status);

    List<SenderID> findByUser(User user);

    Page<SenderID> findByStatus(SenderID.SenderIdStatus status, Pageable pageable);

    boolean existsByUserAndSenderId(User user, String senderId);
}
