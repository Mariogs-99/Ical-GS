package com.hotelJB.hotelJB_API.Dte.company;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private final SecretKeySpec secretKey;
    private static final String ALGORITHM = "AES";

    public EncryptionUtil(@Value("${security.encryption.secretKey}") String secretKeyString) {
        if (secretKeyString == null || secretKeyString.length() != 16) {
            throw new IllegalArgumentException("La clave secreta debe tener exactamente 16 caracteres.");
        }
        this.secretKey = new SecretKeySpec(secretKeyString.getBytes(), ALGORITHM);
    }

    public String encrypt(String input) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(input.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new RuntimeException("Error cifrando dato: " + ex.getMessage(), ex);
        }
    }

    public String decrypt(String encryptedInput) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedInput);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted);
        } catch (Exception ex) {
            throw new RuntimeException("Error descifrando dato: " + ex.getMessage(), ex);
        }
    }
}
