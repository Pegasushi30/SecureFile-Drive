package com.example.securedrive.security;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;


@Component
public class AzureBlobSASTokenGenerator {

    @Value("${azure.storage.connection.string}")
    private String connectionString;

    @Value("${azure.storage.container.name}")
    private String containerName;

    public String getBlobUrl(String blobPath) {
        BlobContainerClient containerClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName);

        BlobClient blobClient = containerClient.getBlobClient(blobPath);
        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues signatureValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusDays(1), permission);
        String sasToken = blobClient.generateSas(signatureValues);
        return blobClient.getBlobUrl() + "?" + sasToken;
    }
}

