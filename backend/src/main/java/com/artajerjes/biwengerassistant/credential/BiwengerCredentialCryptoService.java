package com.artajerjes.biwengerassistant.credential;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BiwengerCredentialCryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";

    private static final byte FORMAT_VERSION = 1;

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public BiwengerCredentialCryptoService(
            @Value("${biwenger.credentials.encryption-key}") String encodedKey) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Biwenger credential encryption key must be valid Base64",
                    exception);
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Biwenger credential encryption key must decode to exactly 32 bytes");
        }

        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String encrypt(String plainToken) {

        if (plainToken == null || plainToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Biwenger token cannot be blank");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv));

            byte[] encrypted = cipher.doFinal(
                    plainToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer payload = ByteBuffer.allocate(
                    1 + iv.length + encrypted.length);

            payload.put(FORMAT_VERSION);
            payload.put(iv);
            payload.put(encrypted);

            return Base64.getEncoder()
                    .encodeToString(payload.array());

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not encrypt Biwenger credential",
                    exception);
        }
    }

    public String decrypt(String encryptedToken) {

        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted Biwenger token cannot be blank");
        }

        try {
            byte[] payload = Base64.getDecoder()
                    .decode(encryptedToken);

            if (payload.length <= 1 + IV_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "Invalid encrypted Biwenger credential");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte version = buffer.get();

            if (version != FORMAT_VERSION) {
                throw new IllegalStateException(
                        "Unsupported Biwenger credential format version: "
                                + version);
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(
                            TAG_LENGTH_BITS,
                            iv));

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8);

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (IllegalArgumentException
                | GeneralSecurityException exception) {

            throw new IllegalStateException(
                    "Could not decrypt Biwenger credential",
                    exception);
        }
    }
}