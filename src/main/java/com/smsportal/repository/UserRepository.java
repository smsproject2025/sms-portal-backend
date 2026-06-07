package com.smsportal.repository;

import com.smsportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    Optional<User> findByApiKey(String apiKey);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetPasswordToken(String token);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    long countAllUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();
}
