package io.github.ozkanpakdil.grepwise.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data.
 * Provides both symmetric (AES-GCM) and asymmetric (RSA) encryption options.
 */
@Service
public class EncryptionService {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${encryption.secret:#{null}}")
    private String encryptionSecret;

    /**
     * Encrypts data using AES-GCM symmetric encryption.
     * Uses a default key if no encryption secret is provided in application properties.
     *
     * @param data The data to encrypt
     * @return Base64 encoded encrypted data
     * @throws GeneralSecurityException If encryption fails
     */
    public String encryptSymmetric(String data) throws GeneralSecurityException {
        if (data == null) {
            return null;
        }

        SecretKey key = getOrCreateSecretKey();
        byte[] iv = generateIv();
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypts data using AES-GCM symmetric encryption.
     * Uses a default key if no encryption secret is provided in application properties.
     *
     * @param encryptedData Base64 encoded encrypted data
     * @return The decrypted data
     * @throws GeneralSecurityException If decryption fails
     */
    public String decryptSymmetric(String encryptedData) throws GeneralSecurityException {
        if (encryptedData == null) {
            return null;
        }

        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);

        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);

        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        SecretKey key = getOrCreateSecretKey();
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        byte[] decryptedData = cipher.doFinal(cipherText);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * Generates a key pair for asymmetric encryption.
     *
     * @return A key pair containing public and private keys
     * @throws GeneralSecurityException If key generation fails
     */
    public KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Encrypts data using RSA asymmetric encryption.
     *
     * @param data      The data to encrypt
     * @param publicKey The public key to use for encryption
     * @return Base64 encoded encrypted data
     * @throws GeneralSecurityException If encryption fails
     */
    public String encryptAsymmetric(String data, PublicKey publicKey) throws GeneralSecurityException {
        if (data == null) {
            return null;
        }

        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * Decrypts data using RSA asymmetric encryption.
     *
     * @param encryptedData The encrypted data
     * @param privateKey    The private key to use for decryption
     * @return The decrypted data
     * @throws GeneralSecurityException If decryption fails
     */
    public String decryptAsymmetric(String encryptedData, PrivateKey privateKey) throws GeneralSecurityException {
        if (encryptedData == null) {
            return null;
        }

        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedData = cipher.doFinal(decodedData);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * Converts a public key to a Base64 encoded string.
     *
     * @param publicKey The public key to convert
     * @return Base64 encoded public key
     */
    public String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Converts a private key to a Base64 encoded string.
     *
     * @param privateKey The private key to convert
     * @return Base64 encoded private key
     */
    public String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Converts a Base64 encoded string to a public key.
     *
     * @param publicKeyString Base64 encoded public key
     * @return The public key
     * @throws GeneralSecurityException If conversion fails
     */
    public PublicKey stringToPublicKey(String publicKeyString) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyString);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Converts a Base64 encoded string to a private key.
     *
     * @param privateKeyString Base64 encoded private key
     * @return The private key
     * @throws GeneralSecurityException If conversion fails
     */
    public PrivateKey stringToPrivateKey(String privateKeyString) throws GeneralSecurityException {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyString);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Gets or creates a secret key for symmetric encryption.
     *
     * @return The secret key
     * @throws GeneralSecurityException If key generation fails
     */
    private SecretKey getOrCreateSecretKey() throws GeneralSecurityException {
        if (encryptionSecret != null && !encryptionSecret.isEmpty()) {
            // Use the provided secret key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } else {
            // Generate a new secret key
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        }
    }

    /**
     * Generates a random initialization vector for AES-GCM encryption.
     *
     * @return The initialization vector
     */
    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}