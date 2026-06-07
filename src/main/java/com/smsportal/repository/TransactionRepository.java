package com.smsportal.repository;

import com.smsportal.model.Transaction;
import com.smsportal.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);

    boolean existsByReferenceId(String referenceId);
}
