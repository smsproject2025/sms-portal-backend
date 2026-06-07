package com.smsportal.repository;

import com.smsportal.model.User;
import com.smsportal.model.WhatsAppLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppLogRepository extends JpaRepository<WhatsAppLog, Long> {

    Page<WhatsAppLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<WhatsAppLog> findByUserAndStatusOrderByCreatedAtDesc(
            User user, WhatsAppLog.MessageStatus status, Pageable pageable);

    Page<WhatsAppLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<WhatsAppLog> findByWaMessageId(String waMessageId);

    List<WhatsAppLog> findByBatchId(String batchId);

    @Query("SELECT COUNT(w) FROM WhatsAppLog w WHERE w.user = :user AND w.status = :status")
    long countByUserAndStatus(@Param("user") User user,
                              @Param("status") WhatsAppLog.MessageStatus status);

    @Query("SELECT SUM(w.cost) FROM WhatsAppLog w WHERE w.user = :user")
    Double sumCostByUser(@Param("user") User user);

    @Query("SELECT COUNT(w) FROM WhatsAppLog w WHERE w.user = :user AND w.createdAt >= :from")
    long countByUserAndCreatedAtAfter(@Param("user") User user,
                                      @Param("from") LocalDateTime from);

    @Query("SELECT DATE(w.createdAt) as date, COUNT(w) as total, " +
           "SUM(CASE WHEN w.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered " +
           "FROM WhatsAppLog w WHERE w.user = :user AND w.createdAt >= :from " +
           "GROUP BY DATE(w.createdAt) ORDER BY DATE(w.createdAt)")
    List<Object[]> getDailyStats(@Param("user") User user, @Param("from") LocalDateTime from);
}
