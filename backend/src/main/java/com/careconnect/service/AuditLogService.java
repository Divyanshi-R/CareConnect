package com.careconnect.service;

import com.careconnect.entity.AuditAction;
import com.careconnect.entity.AuditLog;
import com.careconnect.entity.User;
import com.careconnect.repository.AuditLogRepository;
import com.careconnect.repository.UserRepository;
import com.careconnect.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(AuditAction action, UserDetails actor, String entityType, Long entityId) {
        Long userId = null;
        if (actor instanceof UserPrincipal principal) {
            userId = principal.getId();
        } else if (actor != null) {
            userId = userRepository.findByEmail(actor.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }
        persist(action, userId, entityType, entityId);
    }

    @Transactional
    public void recordLogin(Long userId) {
        persist(AuditAction.LOGIN, userId, "User", userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin(Long attemptedUserId) {
        persist(AuditAction.FAILED_LOGIN, attemptedUserId, "User", attemptedUserId);
    }

    private void persist(AuditAction action, Long userId, String entityType, Long entityId) {
        auditLogRepository.save(new AuditLog(
                userId,
                action,
                entityType,
                entityId,
                LocalDateTime.now(),
                remoteAddress()
        ));
    }

    private String remoteAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
