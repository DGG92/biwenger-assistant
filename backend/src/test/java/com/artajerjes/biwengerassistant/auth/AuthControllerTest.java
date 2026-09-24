package com.artajerjes.biwengerassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.verify;

import com.artajerjes.biwengerassistant.auth.dto.ChangePasswordRequest;
import com.artajerjes.biwengerassistant.auth.dto.CurrentUserResponse;
import com.artajerjes.biwengerassistant.auth.dto.LoginRequest;

class AuthControllerTest {

        private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

        private final AssistantUserRepository assistantUserRepository = mock(AssistantUserRepository.class);

        private final AssistantUserService assistantUserService = mock(AssistantUserService.class);

        private final LoginRateLimitService loginRateLimitService = mock(LoginRateLimitService.class);

        private final AuthController controller = new AuthController(
                        authenticationManager,
                        assistantUserRepository,
                        assistantUserService,
                        loginRateLimitService);

        @Test
        void shouldLoginAndCreateSession() {
                Authentication authentication = mock(Authentication.class);

                when(authentication.getName()).thenReturn("diego");

                when(authenticationManager.authenticate(any(Authentication.class)))
                                .thenReturn(authentication);

                AssistantUser user = new AssistantUser(
                                "diego",
                                "hashed-password",
                                AssistantRole.ADMIN,
                                null);

                when(assistantUserRepository.findByUsernameIgnoreCase("diego"))
                                .thenReturn(Optional.of(user));

                MockHttpServletRequest request = new MockHttpServletRequest();

                MockHttpServletResponse httpResponse = new MockHttpServletResponse();

                CurrentUserResponse response = controller.login(
                                new LoginRequest("diego", "secret"),
                                request,
                                httpResponse);

                assertThat(request.getSession(false)).isNotNull();

                assertThat(response.username()).isEqualTo("diego");
                assertThat(response.role()).isEqualTo(AssistantRole.ADMIN);
                assertThat(response.managerId()).isNull();
                assertThat(response.leagueId()).isNull();
        }

        @Test
        void shouldRejectInvalidCredentials() {
                MockHttpServletResponse response = new MockHttpServletResponse();

                when(authenticationManager.authenticate(any(Authentication.class)))
                                .thenThrow(new BadCredentialsException(
                                                "Bad credentials"));

                MockHttpServletRequest request = new MockHttpServletRequest();

                assertThatThrownBy(() -> controller.login(
                                new LoginRequest("diego", "wrong"),
                                request,
                                response))
                                .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void shouldRejectMeWithoutAuthentication() {
                assertThatThrownBy(() -> controller.me(null))
                                .hasMessageContaining("401");
        }

        @Test
        void shouldLogoutAndInvalidateSession() {
                MockHttpServletRequest request = new MockHttpServletRequest();

                request.getSession(true);

                assertThat(request.getSession(false)).isNotNull();

                controller.logout(request);

                assertThat(request.getSession(false)).isNull();
        }

        @Test
        void shouldLogoutWithoutExistingSession() {
                MockHttpServletRequest request = new MockHttpServletRequest();

                controller.logout(request);

                assertThat(request.getSession(false)).isNull();
        }

        @Test
        void shouldChangeCurrentUserPassword() {
                controller.changePassword(
                                new ChangePasswordRequest(
                                                "new-password",
                                                "new-password"));

                verify(assistantUserService)
                                .changeCurrentUserPassword(
                                                "new-password",
                                                "new-password");
        }

        @Test
        void shouldRejectLoginWhenClientIpIsBlocked() {

                when(loginRateLimitService.isBlocked("127.0.0.1"))
                                .thenReturn(true);

                when(loginRateLimitService.retryAfterSeconds("127.0.0.1"))
                                .thenReturn(600L);

                MockHttpServletRequest request = new MockHttpServletRequest();

                request.setRemoteAddr("127.0.0.1");

                MockHttpServletResponse response = new MockHttpServletResponse();

                assertThatThrownBy(() -> controller.login(
                                new LoginRequest("diego", "secret"),
                                request,
                                response))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("429");

                assertThat(response.getHeader("Retry-After"))
                                .isEqualTo("600");
        }
}