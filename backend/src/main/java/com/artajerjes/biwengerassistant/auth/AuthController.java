package com.artajerjes.biwengerassistant.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.AuthenticationException;

import com.artajerjes.biwengerassistant.auth.dto.CurrentUserResponse;
import com.artajerjes.biwengerassistant.auth.dto.LoginRequest;
import com.artajerjes.biwengerassistant.auth.dto.ChangePasswordRequest;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final AssistantUserRepository assistantUserRepository;
        private final AssistantUserService assistantUserService;
        private final LoginRateLimitService loginRateLimitService;

        public AuthController(
                        AuthenticationManager authenticationManager,
                        AssistantUserRepository assistantUserRepository,
                        AssistantUserService assistantUserService,
                        LoginRateLimitService loginRateLimitService) {

                this.authenticationManager = authenticationManager;
                this.assistantUserRepository = assistantUserRepository;
                this.assistantUserService = assistantUserService;
                this.loginRateLimitService = loginRateLimitService;
        }

        @GetMapping("/csrf")
        public CsrfToken csrf(CsrfToken csrfToken) {
                return csrfToken;
        }

        @PostMapping("/login")
        public CurrentUserResponse login(
                        @RequestBody LoginRequest request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse httpResponse) {

                String clientIp = getClientIp(httpRequest);

                if (loginRateLimitService.isBlocked(clientIp)) {
                        long retryAfter = loginRateLimitService.retryAfterSeconds(clientIp);

                        httpResponse.setHeader(
                                        "Retry-After",
                                        Long.toString(retryAfter));

                        throw new ResponseStatusException(
                                        HttpStatus.TOO_MANY_REQUESTS,
                                        "Too many failed login attempts");
                }

                Authentication authentication;

                try {
                        authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.username(),
                                                        request.password()));
                } catch (AuthenticationException exception) {
                        loginRateLimitService.registerFailure(clientIp);
                        throw exception;
                }

                loginRateLimitService.registerSuccess(clientIp);

                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);

                HttpSession session = httpRequest.getSession(true);

                session.setAttribute(
                                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                securityContext);

                return getCurrentUser(authentication);
        }

        @GetMapping("/me")
        public CurrentUserResponse me(Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Not authenticated");
                }

                return getCurrentUser(authentication);
        }

        private String getClientIp(HttpServletRequest request) {

                String forwardedFor = request.getHeader("X-Forwarded-For");

                if (forwardedFor != null && !forwardedFor.isBlank()) {
                        return forwardedFor.split(",")[0].trim();
                }

                return request.getRemoteAddr();
        }

        private CurrentUserResponse getCurrentUser(
                        Authentication authentication) {

                AssistantUser user = assistantUserRepository
                                .findByUsernameIgnoreCase(authentication.getName())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "Assistant user not found"));

                Long managerId = user.getManager() != null
                                ? user.getManager().getId()
                                : null;

                Long leagueId = user.getManager() != null
                                && user.getManager().getLeague() != null
                                                ? user.getManager().getLeague().getId()
                                                : null;

                return new CurrentUserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getRole(),
                                managerId,
                                leagueId);
        }

        @PostMapping("/change-password")
        public void changePassword(
                        @Valid @RequestBody ChangePasswordRequest request) {

                assistantUserService.changeCurrentUserPassword(
                                request.newPassword(),
                                request.repeatedPassword());
        }

        @PostMapping("/logout")
        public void logout(HttpServletRequest request) {
                HttpSession session = request.getSession(false);

                if (session != null) {
                        session.invalidate();
                }

                SecurityContextHolder.clearContext();
        }
}