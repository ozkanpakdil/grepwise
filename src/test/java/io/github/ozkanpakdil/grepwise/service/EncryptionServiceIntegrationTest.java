package io.github.ozkanpakdil.grepwise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for the EncryptionService.
 * Focuses on more complex scenarios and edge cases.
 */
public class EncryptionServiceIntegrationTest {

    private EncryptionService encryptionService;
    
    @BeforeEach
    public void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "encryptionSecret", "integration-test-secret");
    }

    @Test
    public void testSymmetricEncryptionDecryption() throws GeneralSecurityException {
        // Given
        String originalData = "This is sensitive data for integration testing";

        // When
        String encryptedData = encryptionService.encryptSymmetric(originalData);
        String decryptedData = encryptionService.decryptSymmetric(encryptedData);

        // Then
        assertNotNull(encryptedData);
        assertNotEquals(originalData, encryptedData);
        assertEquals(originalData, decryptedData);
    }

    @Test
    public void testAsymmetricEncryptionDecryption() throws GeneralSecurityException {
        // Given
        String originalData = "This is sensitive data for asymmetric integration testing";
        KeyPair keyPair = encryptionService.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // When
        String encryptedData = encryptionService.encryptAsymmetric(originalData, publicKey);
        String decryptedData = encryptionService.decryptAsymmetric(encryptedData, privateKey);

        // Then
        assertNotNull(encryptedData);
        assertNotEquals(originalData, encryptedData);
        assertEquals(originalData, decryptedData);
    }

    @Test
    public void testKeyConversionAndStorage() throws GeneralSecurityException {
        // Given
        KeyPair keyPair = encryptionService.generateKeyPair();
        PublicKey originalPublicKey = keyPair.getPublic();
        PrivateKey originalPrivateKey = keyPair.getPrivate();

        // When - Convert keys to strings (simulating storage)
        String publicKeyString = encryptionService.publicKeyToString(originalPublicKey);
        String privateKeyString = encryptionService.privateKeyToString(originalPrivateKey);

        // Then - Verify strings are not null and have content
        assertNotNull(publicKeyString);
        assertNotNull(privateKeyString);
        assertTrue(publicKeyString.length() > 0);
        assertTrue(privateKeyString.length() > 0);

        // When - Convert strings back to keys (simulating retrieval)
        PublicKey convertedPublicKey = encryptionService.stringToPublicKey(publicKeyString);
        PrivateKey convertedPrivateKey = encryptionService.stringToPrivateKey(privateKeyString);

        // Then - Verify keys match original keys
        assertEquals(originalPublicKey, convertedPublicKey);
        assertEquals(originalPrivateKey, convertedPrivateKey);

        // When - Use converted keys for encryption/decryption
        String originalData = "Testing with converted keys";
        String encryptedData = encryptionService.encryptAsymmetric(originalData, convertedPublicKey);
        String decryptedData = encryptionService.decryptAsymmetric(encryptedData, convertedPrivateKey);

        // Then - Verify encryption/decryption works with converted keys
        assertEquals(originalData, decryptedData);
    }
}