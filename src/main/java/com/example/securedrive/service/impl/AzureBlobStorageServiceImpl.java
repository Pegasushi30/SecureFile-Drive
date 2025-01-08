package com.example.securedrive.service.impl;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Storage;
import com.example.securedrive.service.AzureBlobStorageService;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class AzureBlobStorageServiceImpl implements AzureBlobStorageService {


    private final BlobContainerClient blobContainerClient;

    @Autowired
    public AzureBlobStorageServiceImpl(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }


    @Override
    public void write(Storage storage) throws AzureBlobStorageException {
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
        } catch (BlobStorageException e) {
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
            return blobClient.exists();
        } catch (BlobStorageException e) {
            log.error("Error while checking blob existence for path '{}': {}", path, e.getServiceMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking blob existence for path '{}': {}", path, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                return false;
            }
            BlobClient blobClient = blobContainerClient.getBlobClient(path);
            return blobClient.exists();
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException("Error checking blob existence: " + e.getServiceMessage());
        } catch (Exception e) {
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

    @Override
    public void createDirectory(String directoryPath) throws AzureBlobStorageException {
        try {
            if (StringUtils.isBlank(directoryPath)) {
                throw new AzureBlobStorageException("Directory path is null or invalid");
            }
            String markerFilePath = directoryPath + ".marker";
            BlobClient blobClient = blobContainerClient.getBlobClient(markerFilePath);
            byte[] emptyContent = new byte[0];
            blobClient.upload(new ByteArrayInputStream(emptyContent), emptyContent.length, true);
            log.info("Directory created with marker at path: {}", markerFilePath);
        } catch (BlobStorageException e) {
            log.error("Azure BlobStorageException: {}", e.getServiceMessage());
            throw new AzureBlobStorageException("Failed to create directory: " + e.getServiceMessage());
        } catch (Exception e) {
            log.error("General exception during directory creation: {}", e.getMessage());
            throw new AzureBlobStorageException("Directory creation failed: " + e.getMessage());
        }
    }
}
