package com.smsportal.repository;

import com.smsportal.model.Transaction;
import com.smsportal.model.User;
import com.smsportal.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUser(User user);

    @Query("SELECT t FROM Transaction t WHERE t.wallet.user = :user ORDER BY t.createdAt DESC")
    Page<Transaction> findTransactionsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.wallet.user = :user AND t.type = 'CREDIT'")
    Double sumTotalRechargeByUser(@Param("user") User user);
}
