package com.company.recruitment.repository;

import com.company.recruitment.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    Optional<EmailMessage> findByGmailMessageId(String gmailMessageId);
    boolean existsByGmailMessageId(String gmailMessageId);
}
