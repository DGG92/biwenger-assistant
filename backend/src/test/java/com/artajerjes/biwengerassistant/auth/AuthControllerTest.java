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

import com.artajerjes.biwengerassistant.auth.dto.CurrentUserResponse;
import com.artajerjes.biwengerassistant.auth.dto.LoginRequest;

class AuthControllerTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);

    private final AssistantUserRepository assistantUserRepository = mock(AssistantUserRepository.class);

    private final AuthController controller = new AuthController(
            authenticationManager,
            assistantUserRepository);

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

        CurrentUserResponse response = controller.login(
                new LoginRequest("diego", "secret"),
                request);

        assertThat(request.getSession(false)).isNotNull();

        assertThat(response.username()).isEqualTo("diego");
        assertThat(response.role()).isEqualTo(AssistantRole.ADMIN);
        assertThat(response.managerId()).isNull();
        assertThat(response.leagueId()).isNull();
    }

    @Test
    void shouldRejectInvalidCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException(
                        "Bad credentials"));

        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> controller.login(
                new LoginRequest("diego", "wrong"),
                request))
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
}