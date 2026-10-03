package com.careconnect.service;

import com.careconnect.dto.LoginRequest;
import com.careconnect.dto.LoginResponse;
import com.careconnect.dto.RegisterRequest;
import com.careconnect.dto.UserResponse;
import com.careconnect.entity.AuditAction;
import com.careconnect.entity.Role;
import com.careconnect.entity.User;
import com.careconnect.exception.DuplicateEmailException;
import com.careconnect.exception.ResourceNotFoundException;
import com.careconnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Value("${app.security.allow-privileged-registration:false}")
    private boolean allowPrivilegedRegistration;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request, UserDetails requestingUser) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Role role = request.getEffectiveRole();
        boolean administrator = requestingUser != null && userRepository.findByEmail(requestingUser.getUsername())
                .map(user -> user.getRole() == Role.ADMIN)
                .orElse(false);

        if (role != Role.PATIENT && !administrator && !allowPrivilegedRegistration) {
            throw new AccessDeniedException("Only administrators can register privileged accounts");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                role,
                true
        );

        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (AuthenticationException exception) {
            Long attemptedUserId = userRepository.findByEmail(normalizedEmail)
                    .map(User::getId)
                    .orElse(null);
            auditLogService.recordFailedLogin(attemptedUserId);
            throw exception;
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));

        String token = jwtService.generateToken(user);
        auditLogService.recordLogin(user.getId());

        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }
}
