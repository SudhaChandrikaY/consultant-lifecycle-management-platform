package com.ensar.clmp.auth.config;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import com.ensar.clmp.auth.service.ClmpUserDetailsService;
import com.ensar.clmp.common.error.ErrorCode;
import com.ensar.clmp.common.error.Problems;

/**
 * Session-based security for the SPA (research R5, R7): JSON login, cookie CSRF, 401/403 as
 * ProblemDetail JSON, and URL-level role rules as defense in depth behind {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ADMIN_MANAGER = { "ADMIN", "MANAGER" };
    private static final String[] COMMERCIAL = { "ADMIN", "MANAGER", "RECRUITER" };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, Environment env) throws Exception {
        boolean dev = env.acceptsProfiles(Profiles.of("dev"));

        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(c -> c.securityContextRepository(securityContextRepository()))
                .csrf(csrf -> {
                    csrf.spa();
                    if (dev) {
                        csrf.ignoringRequestMatchers(PathRequest.toH2Console());
                    }
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> writeProblem(response,
                                ErrorCode.UNAUTHENTICATED, "Your session has expired or you are not signed in."))
                        .accessDeniedHandler((request, response, ex) -> writeProblem(response,
                                ErrorCode.NOT_AUTHORIZED, "You are not authorized to perform this action.")))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/login", "/api/auth/csrf").permitAll();
                    if (dev) {
                        auth.requestMatchers(PathRequest.toH2Console()).hasRole("ADMIN");
                    }
                    auth.requestMatchers("/api/recruiters/**", "/api/reports/**").hasAnyRole(ADMIN_MANAGER)
                            .requestMatchers("/api/marketing-assignments/**", "/api/submissions/**",
                                    "/api/placements/**", "/api/vendors/**", "/api/clients/**")
                            .hasAnyRole(COMMERCIAL)
                            .requestMatchers("/api/consultants/**", "/api/dashboard", "/api/reference",
                                    "/api/auth/**")
                            .authenticated()
                            .requestMatchers("/api/**").authenticated()
                            .requestMatchers("/error").permitAll()
                            .anyRequest().denyAll();
                });

        if (dev) {
            http.headers(h -> h.frameOptions(f -> f.sameOrigin()));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(ClmpUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private static void writeProblem(HttpServletResponse response, ErrorCode code, String detail)
            throws java.io.IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(Problems.json(code, detail));
    }
}
