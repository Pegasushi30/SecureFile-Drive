package com.example.securedrive.service;

import com.example.securedrive.exception.AzureBlobStorageException;
import com.example.securedrive.model.Storage;


public interface AzureBlobStorageService {

    void write(Storage storage) throws AzureBlobStorageException;
    byte[] read(Storage storage) throws AzureBlobStorageException;
    void delete(Storage storage) throws AzureBlobStorageException;
    boolean exists(Storage storage) throws AzureBlobStorageException;
    void createDirectory(String directoryPath) throws AzureBlobStorageException;
}
