package com.ensar.clmp.auth.web;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ensar.clmp.auth.CurrentUserProvider;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.error.ErrorCode;

/**
 * JSON session login (research R5). Every authentication failure, including a disabled user,
 * returns the same generic 401 (AS 1.2, AS 1.6). Request bodies are never logged.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String INVALID_CREDENTIALS = "Invalid username or password.";

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CurrentUserProvider currentUserProvider;
    private final long sessionTimeoutMinutes;

    public AuthController(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository, CurrentUserProvider currentUserProvider,
            @Value("${server.servlet.session.timeout:30m}") Duration sessionTimeout) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.currentUserProvider = currentUserProvider;
        this.sessionTimeoutMinutes = sessionTimeout.toMinutes();
    }

    /** Loads the deferred CSRF token so the XSRF-TOKEN cookie is written. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username().trim(), body.password()));
        } catch (AuthenticationException ex) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, INVALID_CREDENTIALS);
        }

        // Session fixation protection: rotate an existing session id, otherwise start a new session.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        } else {
            request.getSession(true);
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return CurrentUserResponse.from(currentUserProvider.get(), sessionTimeoutMinutes);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return CurrentUserResponse.from(currentUserProvider.get(), sessionTimeoutMinutes);
    }
}
