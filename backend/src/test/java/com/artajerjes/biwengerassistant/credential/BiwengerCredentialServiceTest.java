package com.artajerjes.biwengerassistant.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.credential.dto.BiwengerCredentialStatusResponse;
import com.artajerjes.biwengerassistant.auth.AssistantRole;
import com.artajerjes.biwengerassistant.auth.AssistantUser;
import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;

class BiwengerCredentialServiceTest {

        private BiwengerCredentialRepository repository;
        private BiwengerCredentialCryptoService cryptoService;
        private CurrentAssistantUserService currentAssistantUserService;
        private BiwengerClient biwengerClient;

        private BiwengerCredentialService service;

        @BeforeEach
        void setUp() {

                repository = mock(BiwengerCredentialRepository.class);
                cryptoService = mock(BiwengerCredentialCryptoService.class);
                currentAssistantUserService = mock(CurrentAssistantUserService.class);
                biwengerClient = mock(BiwengerClient.class);

                service = new BiwengerCredentialService(
                                repository,
                                cryptoService,
                                currentAssistantUserService,
                                biwengerClient);
        }

        @Test
        void shouldReturnCurrentUsersBiwengerIdentity() {

                AssistantUser user = createUser(123456L);

                BiwengerCredential credential = new BiwengerCredential(
                                user,
                                123456L,
                                "encrypted-token");

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.of(credential));

                when(cryptoService.decrypt("encrypted-token"))
                                .thenReturn("plain-token");

                BiwengerCredentialService.BiwengerIdentity identity = service.getCurrentIdentity();

                assertThat(identity.userId())
                                .isEqualTo(123456L);

                assertThat(identity.token())
                                .isEqualTo("plain-token");
        }

        @Test
        void shouldRejectUserWithoutManager() {

                AssistantUser user = new AssistantUser(
                                "user",
                                "hashed-password",
                                AssistantRole.USER,
                                null);

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                assertThatThrownBy(service::getCurrentIdentity)
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage(
                                                "Authenticated Assistant user has no manager assigned");
        }

        @Test
        void shouldRejectUserWithoutCredential() {

                AssistantUser user = createUser(123456L);

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(service::getCurrentIdentity)
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage(
                                                "Authenticated Assistant user has no Biwenger credential");
        }

        @Test
        void shouldRejectCredentialBelongingToDifferentBiwengerUser() {

                AssistantUser user = createUser(123456L);

                BiwengerCredential credential = new BiwengerCredential(
                                user,
                                999999L,
                                "encrypted-token");

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.of(credential));

                assertThatThrownBy(service::getCurrentIdentity)
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage(
                                                "Stored Biwenger credential does not match authenticated manager");
        }

        @Test
        void shouldReturnAllStoredBiwengerIdentities() {

                AssistantUser firstUser = createUser(123456L);
                AssistantUser secondUser = createUser(654321L);

                BiwengerCredential firstCredential = new BiwengerCredential(
                                firstUser,
                                123456L,
                                "encrypted-token-one");

                BiwengerCredential secondCredential = new BiwengerCredential(
                                secondUser,
                                654321L,
                                "encrypted-token-two");

                when(repository.findAll())
                                .thenReturn(List.of(
                                                firstCredential,
                                                secondCredential));

                when(cryptoService.decrypt("encrypted-token-one"))
                                .thenReturn("plain-token-one");

                when(cryptoService.decrypt("encrypted-token-two"))
                                .thenReturn("plain-token-two");

                List<BiwengerCredentialService.BiwengerIdentity> identities = service.getAllIdentities();

                assertThat(identities)
                                .containsExactly(
                                                new BiwengerCredentialService.BiwengerIdentity(
                                                                123456L,
                                                                "plain-token-one"),
                                                new BiwengerCredentialService.BiwengerIdentity(
                                                                654321L,
                                                                "plain-token-two"));
        }

        @Test
        void shouldReturnUnlinkedStatusWhenCurrentUserHasNoCredential() {

                AssistantUser user = createUser(123456L);

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.empty());

                BiwengerCredentialStatusResponse result = service.getCurrentCredentialStatus();

                assertThat(result.linked()).isFalse();
                assertThat(result.biwengerUserId()).isNull();
                assertThat(result.lastValidatedAt()).isNull();
        }

        @Test
        void shouldReturnLinkedStatusWithoutExposingToken() {

                AssistantUser user = createUser(123456L);

                BiwengerCredential credential = new BiwengerCredential(
                                user,
                                123456L,
                                "encrypted-secret-token");

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.of(credential));

                BiwengerCredentialStatusResponse result = service.getCurrentCredentialStatus();

                assertThat(result.linked()).isTrue();
                assertThat(result.biwengerUserId()).isEqualTo(123456L);
        }

        @Test
        void shouldValidateEncryptAndSaveNewCredential() {

                AssistantUser user = createUser(123456L);

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.empty());

                when(cryptoService.encrypt("plain-token"))
                                .thenReturn("encrypted-token");

                when(repository.save(org.mockito.ArgumentMatchers.any(
                                BiwengerCredential.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                BiwengerCredentialStatusResponse result = service.saveCurrentCredential("plain-token");

                verify(biwengerClient)
                                .getCurrentUser(
                                                new BiwengerCredentialService.BiwengerIdentity(
                                                                123456L,
                                                                "plain-token"));

                verify(cryptoService)
                                .encrypt("plain-token");

                verify(repository)
                                .save(org.mockito.ArgumentMatchers.argThat(
                                                credential -> credential.getAssistantUser() == user
                                                                && credential.getBiwengerUserId()
                                                                                .equals(123456L)
                                                                && credential.getEncryptedToken()
                                                                                .equals("encrypted-token")
                                                                && credential.getLastValidatedAt() != null));

                assertThat(result.linked()).isTrue();
                assertThat(result.biwengerUserId()).isEqualTo(123456L);
                assertThat(result.lastValidatedAt()).isNotNull();
        }

        @Test
        void shouldReplaceExistingEncryptedTokenAfterSuccessfulValidation() {

                AssistantUser user = createUser(123456L);

                BiwengerCredential existing = new BiwengerCredential(
                                user,
                                123456L,
                                "old-encrypted-token");

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(repository.findByAssistantUser_Id(user.getId()))
                                .thenReturn(Optional.of(existing));

                when(cryptoService.encrypt("new-token"))
                                .thenReturn("new-encrypted-token");

                when(repository.save(existing))
                                .thenReturn(existing);

                service.saveCurrentCredential("new-token");

                verify(biwengerClient)
                                .getCurrentUser(
                                                new BiwengerCredentialService.BiwengerIdentity(
                                                                123456L,
                                                                "new-token"));

                verify(cryptoService)
                                .encrypt("new-token");

                verify(repository)
                                .save(existing);

                assertThat(existing.getEncryptedToken())
                                .isEqualTo("new-encrypted-token");

                assertThat(existing.getLastValidatedAt())
                                .isNotNull();
        }

        @Test
        void shouldRejectBlankTokenWithoutCallingBiwenger() {

                assertThatThrownBy(
                                () -> service.saveCurrentCredential("   "))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessage("Biwenger token cannot be blank");

                verify(biwengerClient, never())
                                .getCurrentUser(
                                                org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldRejectSavingCredentialWhenUserHasNoManager() {

                AssistantUser user = new AssistantUser(
                                "user",
                                "hashed-password",
                                AssistantRole.USER,
                                null);

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                assertThatThrownBy(
                                () -> service.saveCurrentCredential("plain-token"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage(
                                                "Authenticated Assistant user has no manager assigned");

                verify(biwengerClient, never())
                                .getCurrentUser(
                                                org.mockito.ArgumentMatchers.any());
        }

        @Test
        void shouldNotPersistCredentialWhenBiwengerValidationFails() {

                AssistantUser user = createUser(123456L);

                BiwengerCredentialService.BiwengerIdentity identity = new BiwengerCredentialService.BiwengerIdentity(
                                123456L,
                                "invalid-token");

                when(currentAssistantUserService.getCurrentUser())
                                .thenReturn(user);

                when(biwengerClient.getCurrentUser(identity))
                                .thenThrow(
                                                new HttpClientErrorException(
                                                                HttpStatus.UNAUTHORIZED,
                                                                "Unauthorized"));

                assertThatThrownBy(
                                () -> service.saveCurrentCredential("invalid-token"))
                                .isInstanceOf(InvalidBiwengerCredentialException.class)
                                .hasMessage("Invalid Biwenger credential");

                verify(cryptoService, never())
                                .encrypt(
                                                org.mockito.ArgumentMatchers.anyString());

                verify(repository, never())
                                .save(
                                                org.mockito.ArgumentMatchers.any(
                                                                BiwengerCredential.class));
        }

        private AssistantUser createUser(Long biwengerManagerId) {

                League league = new League(
                                "Liga amigos",
                                "12345");

                Manager manager = new Manager(
                                biwengerManagerId,
                                "Manager",
                                null,
                                100,
                                20,
                                50_000_000L,
                                100_000L,
                                1,
                                "manager",
                                league);

                return new AssistantUser(
                                "user",
                                "hashed-password",
                                AssistantRole.USER,
                                manager);
        }
}