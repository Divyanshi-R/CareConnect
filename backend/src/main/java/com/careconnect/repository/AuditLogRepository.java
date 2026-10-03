package com.careconnect.repository;

import com.careconnect.entity.AuditAction;
import com.careconnect.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAction(AuditAction action);
}
