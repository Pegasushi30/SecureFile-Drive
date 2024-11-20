package com.example.securedrive.service.impl;

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

    @Override
    public String update(Storage storage) throws AzureBlobStorageException {
        // write metodu ile aynı işlemi yapabilir
        return write(storage);
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
            client.download(outputStream);
            return outputStream.toByteArray();
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            throw new AzureBlobStorageException(e.getMessage());
        }
    }

    @Override
    public List<String> listFiles(Storage storage) throws AzureBlobStorageException {
        try {
            String path = storage.getFullPath();
            if (StringUtils.isBlank(path)) {
                throw new AzureBlobStorageException("Storage path is null or invalid");
            }
            PagedIterable<BlobItem> blobList = blobContainerClient.listBlobsByHierarchy(path + "/");
            List<String> blobNamesList = new ArrayList<>();
            for (BlobItem blob : blobList) {
                blobNamesList.add(blob.getName());
            }
            return blobNamesList;
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
    public void createContainer() throws AzureBlobStorageException {
        try {
            blobServiceClient.createBlobContainer(containerName);
            log.info("Container Created");
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            throw new AzureBlobStorageException(e.getMessage());
        }
    }

    @Override
    public void deleteContainer() throws AzureBlobStorageException {
        try {
            blobServiceClient.deleteBlobContainer(containerName);
            log.info("Container Deleted");
        } catch (BlobStorageException e) {
            throw new AzureBlobStorageException(e.getServiceMessage());
        } catch (Exception e) {
            throw new AzureBlobStorageException(e.getMessage());
        }
    }
}
