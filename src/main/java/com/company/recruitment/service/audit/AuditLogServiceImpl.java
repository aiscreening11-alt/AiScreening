package com.company.recruitment.service.audit;

import com.company.recruitment.entity.AuditLog;
import com.company.recruitment.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public AuditLog logAction(String entityType, String entityId, String action, String performedBy, String oldState, String newState, String reason) {
        log.info("Audit: [{}] {} on entity {} id={}. PerformedBy: {}. Reason: {}", entityType, action, entityType, entityId, performedBy, reason);
        AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .oldState(oldState)
                .newState(newState)
                .reason(reason)
                .build();
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsForEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }
}
