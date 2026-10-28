package com.company.recruitment.repository;

import com.company.recruitment.entity.EmailCommunication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmailCommunicationRepository extends JpaRepository<EmailCommunication, UUID> {
    List<EmailCommunication> findByCandidateIdOrderBySentAtDesc(UUID candidateId);
    List<EmailCommunication> findByRecipientEmailIgnoreCaseOrderBySentAtDesc(String recipientEmail);
}
