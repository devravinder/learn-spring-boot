package com.paravar.data.service;

import com.paravar.data.model.AuditLog;
import com.paravar.data.repo.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// a separate bean: self-calls skip the proxy, so REQUIRES_NEW would be ignored
@Service
public class AuditService {

    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String message) {
        logs.save(new AuditLog(message));
    }

    @Transactional(readOnly = true)
    public List<String> all() {
        return logs.findAll().stream().map(AuditLog::getMessage).toList();
    }
}
