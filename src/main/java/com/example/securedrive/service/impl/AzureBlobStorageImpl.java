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

    @Override
    public List<String> listBlobs(String directoryPath) throws AzureBlobStorageException {
        try {
            if (StringUtils.isBlank(directoryPath)) {
                throw new AzureBlobStorageException("Directory path is null or invalid");
            }
            List<String> blobNames = new ArrayList<>();
            PagedIterable<BlobItem> blobs = blobContainerClient.listBlobsByHierarchy(directoryPath);
            for (BlobItem blobItem : blobs) {
                blobNames.add(blobItem.getName());
            }
            return blobNames;
        } catch (BlobStorageException e) {
            log.error("Azure BlobStorageException: {}", e.getServiceMessage());
            throw new AzureBlobStorageException("Failed to list blobs: " + e.getServiceMessage());
        } catch (Exception e) {
            log.error("General exception during blob listing: {}", e.getMessage());
            throw new AzureBlobStorageException("Blob listing failed: " + e.getMessage());
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

    @Override
    public void deleteDirectory(String directoryPath) throws AzureBlobStorageException {
        try {
            if (StringUtils.isBlank(directoryPath)) {
                throw new AzureBlobStorageException("Directory path is null or invalid");
            }

            // Altındaki tüm blob'ları listele ve sil
            String fullDirectoryPath = directoryPath.endsWith("/") ? directoryPath : directoryPath + "/";
            PagedIterable<BlobItem> blobs = blobContainerClient.listBlobs(); // Tüm blobları listele

            for (BlobItem blobItem : blobs) {
                String blobName = blobItem.getName();

                // Belirtilen directoryPath'e ait olanları filtrele
                if (!blobName.startsWith(fullDirectoryPath)) {
                    continue;
                }

                BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

                // Marker, versiyon ve diğer blobları kontrol et
                if (blobName.endsWith(".marker")) {
                    log.info("Deleting marker file: {}", blobName);
                } else if (blobName.contains("/versions/")) {
                    log.info("Deleting versioned blob: {}", blobName);
                } else {
                    log.info("Deleting blob: {}", blobName);
                }

                if (blobClient.exists()) {
                    blobClient.delete();
                    log.info("Deleted blob: {}", blobName);
                } else {
                    log.warn("Blob does not exist: {}", blobName);
                }
            }

            log.info("Directory and its contents deleted successfully: {}", fullDirectoryPath);
        } catch (BlobStorageException e) {
            log.error("Azure BlobStorageException: {}", e.getServiceMessage());
            throw new AzureBlobStorageException("Failed to delete directory: " + e.getServiceMessage());
        } catch (Exception e) {
            log.error("General exception during directory deletion: {}", e.getMessage());
            throw new AzureBlobStorageException("Directory deletion failed: " + e.getMessage());
        }
    }



}
