package com.artajerjes.biwengerassistant.credential;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import com.artajerjes.biwengerassistant.auth.AssistantUser;
import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.credential.dto.BiwengerCredentialStatusResponse;

@Service
public class BiwengerCredentialService {

        private final BiwengerCredentialRepository biwengerCredentialRepository;
        private final BiwengerCredentialCryptoService cryptoService;
        private final CurrentAssistantUserService currentAssistantUserService;
        private final BiwengerClient biwengerClient;

        public BiwengerCredentialService(
                        BiwengerCredentialRepository biwengerCredentialRepository,
                        BiwengerCredentialCryptoService cryptoService,
                        CurrentAssistantUserService currentAssistantUserService,
                        BiwengerClient biwengerClient) {

                this.biwengerCredentialRepository = biwengerCredentialRepository;
                this.cryptoService = cryptoService;
                this.currentAssistantUserService = currentAssistantUserService;
                this.biwengerClient = biwengerClient;
        }

        @Transactional(readOnly = true)
        public BiwengerCredentialStatusResponse getCurrentCredentialStatus() {

                AssistantUser user = currentAssistantUserService.getCurrentUser();

                return biwengerCredentialRepository
                                .findByAssistantUser_Id(user.getId())
                                .map(credential -> new BiwengerCredentialStatusResponse(
                                                true,
                                                credential.getBiwengerUserId(),
                                                credential.getLastValidatedAt()))
                                .orElseGet(() -> new BiwengerCredentialStatusResponse(
                                                false,
                                                null,
                                                null));
        }

        @Transactional
        public BiwengerCredentialStatusResponse saveCurrentCredential(
                        String rawToken) {

                if (rawToken == null || rawToken.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Biwenger token cannot be blank");
                }

                AssistantUser user = currentAssistantUserService.getCurrentUser();
                Manager manager = user.getManager();

                if (manager == null) {
                        throw new IllegalStateException(
                                        "Authenticated Assistant user has no manager assigned");
                }

                Long biwengerUserId = manager.getBiwengerManagerId();

                BiwengerIdentity identity = new BiwengerIdentity(
                                biwengerUserId,
                                rawToken.trim());

                /*
                 * Validation happens before anything is persisted.
                 * Biwenger rejects a mismatched token/user pair.
                 */
                try {
                        biwengerClient.getCurrentUser(identity);
                } catch (HttpClientErrorException exception) {
                        if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                                throw new InvalidBiwengerCredentialException(exception);
                        }

                        throw exception;
                }

                String encryptedToken = cryptoService.encrypt(
                                rawToken.trim());

                BiwengerCredential credential = biwengerCredentialRepository
                                .findByAssistantUser_Id(user.getId())
                                .map(existing -> {
                                        existing.updateToken(encryptedToken);
                                        return existing;
                                })
                                .orElseGet(() -> new BiwengerCredential(
                                                user,
                                                biwengerUserId,
                                                encryptedToken));

                credential.markValidated();

                BiwengerCredential saved = biwengerCredentialRepository.save(
                                credential);

                return new BiwengerCredentialStatusResponse(
                                true,
                                saved.getBiwengerUserId(),
                                saved.getLastValidatedAt());
        }

        @Transactional(readOnly = true)
        public BiwengerIdentity getCurrentIdentity() {

                AssistantUser user = currentAssistantUserService.getCurrentUser();
                Manager manager = user.getManager();

                if (manager == null) {
                        throw new IllegalStateException(
                                        "Authenticated Assistant user has no manager assigned");
                }

                BiwengerCredential credential = biwengerCredentialRepository
                                .findByAssistantUser_Id(user.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Authenticated Assistant user has no Biwenger credential"));

                if (!manager.getBiwengerManagerId()
                                .equals(credential.getBiwengerUserId())) {

                        throw new IllegalStateException(
                                        "Stored Biwenger credential does not match authenticated manager");
                }

                return new BiwengerIdentity(
                                credential.getBiwengerUserId(),
                                cryptoService.decrypt(
                                                credential.getEncryptedToken()));
        }

        @Transactional(readOnly = true)
        public List<BiwengerIdentity> getAllIdentities() {

                return biwengerCredentialRepository.findAll()
                                .stream()
                                .map(credential -> new BiwengerIdentity(
                                                credential.getBiwengerUserId(),
                                                cryptoService.decrypt(
                                                                credential.getEncryptedToken())))
                                .toList();
        }

        public record BiwengerIdentity(
                        Long userId,
                        String token) {
        }
}