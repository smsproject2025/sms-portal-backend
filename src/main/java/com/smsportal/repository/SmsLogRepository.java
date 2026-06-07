package com.smsportal.repository;

import com.smsportal.model.SmsLog;
import com.smsportal.model.User;
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
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    Page<SmsLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<SmsLog> findByUserAndStatusOrderByCreatedAtDesc(User user, SmsLog.SmsStatus status, Pageable pageable);

    Page<SmsLog> findByUserAndTypeOrderByCreatedAtDesc(User user, SmsLog.SmsType type, Pageable pageable);

    Optional<SmsLog> findByGatewayMessageId(String gatewayMessageId);

    List<SmsLog> findByBatchId(String batchId);

    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.user = :user AND s.createdAt >= :from")
    long countByUserAndCreatedAtAfter(@Param("user") User user, @Param("from") LocalDateTime from);

    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.user = :user AND s.status = :status")
    long countByUserAndStatus(@Param("user") User user, @Param("status") SmsLog.SmsStatus status);

    @Query("SELECT SUM(s.cost) FROM SmsLog s WHERE s.user = :user")
    Double sumCostByUser(@Param("user") User user);

    @Query("SELECT COUNT(s) FROM SmsLog s WHERE s.createdAt >= :from")
    long countAllSentAfter(@Param("from") LocalDateTime from);

    @Query("SELECT s.status, COUNT(s) FROM SmsLog s WHERE s.user = :user GROUP BY s.status")
    List<Object[]> getStatusCountByUser(@Param("user") User user);

    @Query("SELECT DATE(s.createdAt) as date, COUNT(s) as total, " +
           "SUM(CASE WHEN s.status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered " +
           "FROM SmsLog s WHERE s.user = :user AND s.createdAt >= :from " +
           "GROUP BY DATE(s.createdAt) ORDER BY DATE(s.createdAt)")
    List<Object[]> getDailyStats(@Param("user") User user, @Param("from") LocalDateTime from);

    Page<SmsLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
