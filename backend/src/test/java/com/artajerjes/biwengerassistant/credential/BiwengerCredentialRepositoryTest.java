package com.artajerjes.biwengerassistant.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.auth.AssistantRole;
import com.artajerjes.biwengerassistant.auth.AssistantUser;
import com.artajerjes.biwengerassistant.auth.AssistantUserRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class BiwengerCredentialRepositoryTest {

    @Autowired
    private BiwengerCredentialRepository biwengerCredentialRepository;

    @Autowired
    private AssistantUserRepository assistantUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindCredentialByAssistantUserId() {

        AssistantUser user = assistantUserRepository.save(
                new AssistantUser(
                        "credential-user",
                        "hashed-password",
                        AssistantRole.USER,
                        null));

        BiwengerCredential credential = biwengerCredentialRepository.save(
                new BiwengerCredential(
                        user,
                        123456L,
                        "encrypted-token"));

        entityManager.flush();
        entityManager.clear();

        Optional<BiwengerCredential> result = biwengerCredentialRepository.findByAssistantUser_Id(
                user.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId())
                .isEqualTo(credential.getId());
        assertThat(result.get().getBiwengerUserId())
                .isEqualTo(123456L);
        assertThat(result.get().getEncryptedToken())
                .isEqualTo("encrypted-token");
        assertThat(result.get().getCreatedAt())
                .isNotNull();
        assertThat(result.get().getUpdatedAt())
                .isNotNull();
        assertThat(result.get().getLastValidatedAt())
                .isNull();
    }

    @Test
    void shouldDetectCredentialByAssistantUserId() {

        AssistantUser user = assistantUserRepository.save(
                new AssistantUser(
                        "credential-exists",
                        "hashed-password",
                        AssistantRole.USER,
                        null));

        biwengerCredentialRepository.save(
                new BiwengerCredential(
                        user,
                        654321L,
                        "encrypted-token"));

        entityManager.flush();

        assertThat(
                biwengerCredentialRepository
                        .existsByAssistantUser_Id(user.getId()))
                .isTrue();
    }

    @Test
    void shouldNotAllowTwoCredentialsForSameAssistantUser() {

        AssistantUser user = assistantUserRepository.save(
                new AssistantUser(
                        "duplicate-credential",
                        "hashed-password",
                        AssistantRole.USER,
                        null));

        biwengerCredentialRepository.saveAndFlush(
                new BiwengerCredential(
                        user,
                        111111L,
                        "encrypted-token-one"));

        assertThatThrownBy(() -> biwengerCredentialRepository.saveAndFlush(
                new BiwengerCredential(
                        user,
                        222222L,
                        "encrypted-token-two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}