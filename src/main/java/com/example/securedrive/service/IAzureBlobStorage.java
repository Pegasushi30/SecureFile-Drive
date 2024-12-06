package com.example.securedrive.service;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Storage;

import java.util.List;

public interface IAzureBlobStorage {

    String write(Storage storage) throws AzureBlobStorageException;
    byte[] read(Storage storage) throws AzureBlobStorageException;
    void delete(Storage storage) throws AzureBlobStorageException;
    boolean exists(Storage storage) throws AzureBlobStorageException;

    void createDirectory(String directoryPath) throws AzureBlobStorageException;
    List<String> listBlobs(String directoryPath) throws AzureBlobStorageException;
    void deleteDirectory(String directoryPath) throws AzureBlobStorageException;
}
