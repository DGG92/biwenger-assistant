package com.artajerjes.biwengerassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;

class AssistantUserServiceTest {

    private final AssistantUserRepository assistantUserRepository = mock(AssistantUserRepository.class);

    private final ManagerRepository managerRepository = mock(ManagerRepository.class);

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final AssistantUserService service = new AssistantUserService(
            assistantUserRepository,
            managerRepository,
            passwordEncoder);

    @Test
    void shouldCreateAdminWithEncodedPassword() {
        when(assistantUserRepository
                .existsByUsernameIgnoreCase("diego"))
                .thenReturn(false);

        when(passwordEncoder.encode("secret"))
                .thenReturn("encoded-secret");

        when(assistantUserRepository.save(
                org.mockito.ArgumentMatchers.any(AssistantUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssistantUser result = service.create(
                "diego",
                "secret",
                AssistantRole.ADMIN,
                null);

        assertThat(result.getUsername()).isEqualTo("diego");
        assertThat(result.getPasswordHash())
                .isEqualTo("encoded-secret");
        assertThat(result.getRole())
                .isEqualTo(AssistantRole.ADMIN);
        assertThat(result.isEnabled()).isTrue();

        verify(passwordEncoder).encode("secret");
    }

    @Test
    void shouldAssociateExistingManager() {
        Manager manager = mock(Manager.class);

        when(assistantUserRepository
                .existsByUsernameIgnoreCase("diego"))
                .thenReturn(false);

        when(managerRepository.findById(7L))
                .thenReturn(Optional.of(manager));

        when(passwordEncoder.encode("secret"))
                .thenReturn("encoded-secret");

        when(assistantUserRepository.save(
                org.mockito.ArgumentMatchers.any(AssistantUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssistantUser result = service.create(
                "diego",
                "secret",
                AssistantRole.ADMIN,
                7L);

        assertThat(result.getManager()).isSameAs(manager);
    }

    @Test
    void shouldRejectDuplicatedUsername() {
        when(assistantUserRepository
                .existsByUsernameIgnoreCase("diego"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                "diego",
                "secret",
                AssistantRole.ADMIN,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectUnknownManager() {
        when(assistantUserRepository
                .existsByUsernameIgnoreCase("diego"))
                .thenReturn(false);

        when(managerRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                "diego",
                "secret",
                AssistantRole.ADMIN,
                999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Manager not found");
    }
}