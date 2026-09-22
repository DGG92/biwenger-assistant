package com.artajerjes.biwengerassistant.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BiwengerCredentialCryptoServiceTest {

    private BiwengerCredentialCryptoService cryptoService;

    @BeforeEach
    void setUp() {

        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);

        cryptoService = new BiwengerCredentialCryptoService(
                Base64.getEncoder().encodeToString(key));
    }

    @Test
    void encryptAndDecryptShouldRecoverOriginalToken() {

        String token = "test-biwenger-token";

        String encrypted = cryptoService.encrypt(token);

        assertNotEquals(token, encrypted);
        assertEquals(
                token,
                cryptoService.decrypt(encrypted));
    }

    @Test
    void encryptingSameTokenTwiceShouldProduceDifferentPayloads() {

        String token = "same-token";

        String first = cryptoService.encrypt(token);
        String second = cryptoService.encrypt(token);

        assertNotEquals(first, second);

        assertEquals(
                token,
                cryptoService.decrypt(first));

        assertEquals(
                token,
                cryptoService.decrypt(second));
    }

    @Test
    void decryptShouldRejectTamperedPayload() {

        String encrypted = cryptoService.encrypt(
                "test-token");

        byte[] payload = Base64.getDecoder()
                .decode(encrypted);

        payload[payload.length - 1] ^= 1;

        String tampered = Base64.getEncoder()
                .encodeToString(payload);

        assertThrows(
                IllegalStateException.class,
                () -> cryptoService.decrypt(tampered));
    }

    @Test
    void constructorShouldRejectInvalidKeyLength() {

        String invalidKey = Base64.getEncoder()
                .encodeToString(new byte[16]);

        assertThrows(
                IllegalStateException.class,
                () -> new BiwengerCredentialCryptoService(
                        invalidKey));
    }

    @Test
    void encryptShouldRejectBlankToken() {

        assertThrows(
                IllegalArgumentException.class,
                () -> cryptoService.encrypt(" "));
    }
}