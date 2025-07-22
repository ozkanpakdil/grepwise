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
 * Unit tests for the EncryptionService.
 */
public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    public void setUp() {
        encryptionService = new EncryptionService();
        // Set a test encryption secret
        ReflectionTestUtils.setField(encryptionService, "encryptionSecret", "test-encryption-secret");
    }

    @Test
    public void testSymmetricEncryptionDecryption() throws GeneralSecurityException {
        // Given
        String originalData = "This is sensitive data that needs to be encrypted";

        // When
        String encryptedData = encryptionService.encryptSymmetric(originalData);
        String decryptedData = encryptionService.decryptSymmetric(encryptedData);

        // Then
        assertNotNull(encryptedData);
        assertNotEquals(originalData, encryptedData);
        assertEquals(originalData, decryptedData);
    }

    @Test
    public void testSymmetricEncryptionWithNullData() throws GeneralSecurityException {
        // When
        String encryptedData = encryptionService.encryptSymmetric(null);

        // Then
        assertNull(encryptedData);
    }

    @Test
    public void testSymmetricDecryptionWithNullData() throws GeneralSecurityException {
        // When
        String decryptedData = encryptionService.decryptSymmetric(null);

        // Then
        assertNull(decryptedData);
    }

    @Test
    public void testAsymmetricEncryptionDecryption() throws GeneralSecurityException {
        // Given
        String originalData = "This is sensitive data that needs to be encrypted asymmetrically";
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
    public void testAsymmetricEncryptionWithNullData() throws GeneralSecurityException {
        // Given
        KeyPair keyPair = encryptionService.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();

        // When
        String encryptedData = encryptionService.encryptAsymmetric(null, publicKey);

        // Then
        assertNull(encryptedData);
    }

    @Test
    public void testAsymmetricDecryptionWithNullData() throws GeneralSecurityException {
        // Given
        KeyPair keyPair = encryptionService.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        // When
        String decryptedData = encryptionService.decryptAsymmetric(null, privateKey);

        // Then
        assertNull(decryptedData);
    }

    @Test
    public void testKeyConversion() throws GeneralSecurityException {
        // Given
        KeyPair keyPair = encryptionService.generateKeyPair();
        PublicKey originalPublicKey = keyPair.getPublic();
        PrivateKey originalPrivateKey = keyPair.getPrivate();

        // When
        String publicKeyString = encryptionService.publicKeyToString(originalPublicKey);
        String privateKeyString = encryptionService.privateKeyToString(originalPrivateKey);
        PublicKey convertedPublicKey = encryptionService.stringToPublicKey(publicKeyString);
        PrivateKey convertedPrivateKey = encryptionService.stringToPrivateKey(privateKeyString);

        // Then
        assertNotNull(publicKeyString);
        assertNotNull(privateKeyString);
        assertEquals(originalPublicKey, convertedPublicKey);
        assertEquals(originalPrivateKey, convertedPrivateKey);
    }

    @Test
    public void testEndToEndEncryptionWithKeyConversion() throws GeneralSecurityException {
        // Given
        String originalData = "This is a complete end-to-end encryption test";
        KeyPair keyPair = encryptionService.generateKeyPair();
        
        // Convert keys to strings (simulating storage)
        String publicKeyString = encryptionService.publicKeyToString(keyPair.getPublic());
        String privateKeyString = encryptionService.privateKeyToString(keyPair.getPrivate());
        
        // Convert strings back to keys (simulating retrieval)
        PublicKey publicKey = encryptionService.stringToPublicKey(publicKeyString);
        PrivateKey privateKey = encryptionService.stringToPrivateKey(privateKeyString);

        // When
        String encryptedData = encryptionService.encryptAsymmetric(originalData, publicKey);
        String decryptedData = encryptionService.decryptAsymmetric(encryptedData, privateKey);

        // Then
        assertEquals(originalData, decryptedData);
    }
}