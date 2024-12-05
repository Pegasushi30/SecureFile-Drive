package com.example.securedrive.security;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeyVaultService {

    private final SecretClient secretClient;

    public KeyVaultService(@Value("${spring.cloud.azure.keyvault.secret.endpoint}") String keyVaultUri,
                           @Value("${b2c.client-id}") String clientId,
                           @Value("${b2c.client-secret}") String clientSecret,
                           @Value("${b2c.tenant-id}") String tenantId) {
        this.secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new ClientSecretCredentialBuilder()
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .tenantId(tenantId)
                        .build())
                .buildClient();
    }

    public void saveEncryptionKeyToKeyVault(String userId, String encryptionKey) {
        String secretName = "aes-key-" + userId;
        secretClient.setSecret(secretName, encryptionKey);
        System.out.println("AES anahtarı Key Vault'a başarıyla kaydedildi: " + secretName);
    }

    public String getEncryptionKeyFromKeyVault(String userId) {
        String secretName = "aes-key-" + userId;
        try {
            return secretClient.getSecret(secretName).getValue();
        } catch (ResourceNotFoundException e) {
            System.err.println("AES anahtarı bulunamadı: " + secretName);
            throw new IllegalStateException("Encryption key not found in Key Vault for user: " + userId, e);
        }
    }
}
