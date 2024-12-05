package com.example.securedrive.service.impl;

import com.azure.storage.blob.BlobClientBuilder;
import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Storage;
import com.example.securedrive.service.IAzureBlobStorage;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

@Service
@Slf4j
public class AzureBlobStorageImpl implements IAzureBlobStorage {

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private BlobContainerClient blobContainerClient;

    @Value("${azure.storage.container.name}")
    private String containerName;

    @Override
    public String write(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                throw new AzureBlobStorageException("Storage path is null or invalid");
            }
            BlobClient blob = blobContainerClient.getBlobClient(path);

            if (storage.getData() != null) {
                blob.upload(new ByteArrayInputStream(storage.getData()), storage.getData().length, true);
            } else {
                throw new AzureBlobStorageException("Storage has no data to upload");
            }

            log.info("Successfully uploaded blob to path: {}", path);
            return path;
        } catch (BlobStorageException e) {
            log.error("Azure BlobStorageException: {}", e.getServiceMessage());
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            log.error("General exception during blob upload: {}", e.getMessage());
            throw new AzureBlobStorageException("Blob upload failed: " + e.getMessage());
        }
    }

    public boolean checkBlobExists(String path) {
        try {
            if (StringUtils.isBlank(path)) {
                log.warn("Invalid path provided for existence check: {}", path);
                return false;
            }

            BlobClient blobClient = blobContainerClient.getBlobClient(path);
            boolean exists = blobClient.exists();
            log.info("Blob existence check for path '{}': {}", path, exists);
            return exists;
        } catch (BlobStorageException e) {
            log.error("Error while checking blob existence for path '{}': {}", path, e.getServiceMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking blob existence for path '{}': {}", path, e.getMessage());
            return false;
        }
    }


    // Yeni eklenen exists metodunu burada bulabilirsiniz.
    @Override
    public boolean exists(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                log.warn("Invalid path provided for existence check: {}", path);
                return false;
            }

            BlobClient blobClient = blobContainerClient.getBlobClient(path);
            boolean exists = blobClient.exists();
            log.info("Blob existence check for path '{}': {}", path, exists);
            return exists;
        } catch (BlobStorageException e) {
            log.error("Error while checking blob existence for path '{}': {}", path, e.getServiceMessage());
            throw new AzureBlobStorageException("Error checking blob existence: " + e.getServiceMessage());
        } catch (Exception e) {
            log.error("Unexpected error while checking blob existence for path '{}': {}", path, e.getMessage());
            throw new AzureBlobStorageException("Unexpected error checking blob existence: " + e.getMessage());
        }
    }

    @Override
    public byte[] read(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                throw new AzureBlobStorageException("Storage path is null or invalid");
            }
            BlobClient client = blobContainerClient.getBlobClient(path);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            client.downloadStream(outputStream);
            return outputStream.toByteArray();
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            throw new AzureBlobStorageException(e.getMessage());
        }
    }

    @Override
    public void delete(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                throw new AzureBlobStorageException("Storage path is null or invalid");
            }
            BlobClient client = blobContainerClient.getBlobClient(path);
            client.delete();
            log.info("Blob is deleted successfully at path: {}", path);
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            throw new AzureBlobStorageException(e.getMessage());
        }
    }
    public byte[] readFromSasUrl(String sasUrl) {
        BlobClient blobClient = new BlobClientBuilder()
                .endpoint(sasUrl)
                .buildClient();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.downloadStream(outputStream);
        return outputStream.toByteArray();
    }
}
