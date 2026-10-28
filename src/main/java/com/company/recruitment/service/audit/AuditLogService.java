package com.company.recruitment.service.audit;

import com.company.recruitment.entity.AuditLog;

import java.util.List;

public interface AuditLogService {
    AuditLog logAction(String entityType, String entityId, String action, String performedBy, String oldState, String newState, String reason);
    List<AuditLog> getLogsForEntity(String entityType, String entityId);
}
