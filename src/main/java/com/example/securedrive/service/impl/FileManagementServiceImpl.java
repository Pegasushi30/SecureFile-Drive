// com/example/securedrive/service/impl/FileManagementServiceImpl.java
package com.example.securedrive.service.impl;

import com.example.securedrive.dto.FileDto;
import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.dto.FileVersionDto;
import com.example.securedrive.mapper.FileMapper;
import com.example.securedrive.mapper.FileShareMapper;
import com.example.securedrive.model.*;
import com.example.securedrive.repository.FileRepository;
import com.example.securedrive.repository.FileShareRepository;
import com.example.securedrive.repository.FileVersionRepository;
import com.example.securedrive.repository.UserRepository;
import com.example.securedrive.security.AzureBlobSASTokenGenerator;
import com.example.securedrive.service.FileManagementService;
import com.example.securedrive.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileManagementServiceImpl implements FileManagementService {

    private final FileRepository fileRepository;
    private final FileShareRepository fileShareRepository;
    private final AzureBlobSASTokenGenerator azureBlobSASTokenGenerator;
    private final FileMapper fileMapper;
    private final UserManagementService userManagementService;
    private final FileShareMapper fileShareMapper;
    private final FileVersionRepository fileVersionRepository;
    private final UserRepository userRepository;

    @Autowired
    public FileManagementServiceImpl(FileRepository fileRepository,
                                     FileShareRepository fileShareRepository,
                                     AzureBlobSASTokenGenerator azureBlobSASTokenGenerator,
                                     FileMapper fileMapper,
                                     UserManagementService userManagementService,
                                     FileShareMapper fileShareMapper,
                                     FileVersionRepository fileVersionRepository,
                                     UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.fileShareRepository = fileShareRepository;
        this.azureBlobSASTokenGenerator = azureBlobSASTokenGenerator;
        this.fileMapper = fileMapper;
        this.userManagementService = userManagementService;
        this.fileShareMapper = fileShareMapper;
        this.fileVersionRepository = fileVersionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<File> getFilesByUser(User user) {
        return fileRepository.findAllByUser(user);
    }

    @Override
    public void saveFile(File file) {
        fileRepository.save(file);
    }

    @Override
    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }

    @Override
    public Optional<File> findByIdAndUser(Long id, User user) {
        return fileRepository.findByIdAndUser(id, user);
    }

    @Override
    public File findByFileNameAndUserDirectoryNull(String fileName, User user) {
        return fileRepository.findByUserAndFileNameAndDirectoryIsNull(user, fileName).orElse(null);
    }

    @Override
    public File findByFileNameUserAndDirectory(String fileName, User user, Directory directory) {
        return fileRepository.findByUserAndFileNameAndDirectory(user, fileName, directory).orElse(null);
    }

    @Override
    public List<File> getUserFilesInRootDirectory(User user) {
        return fileRepository.findAllByUserAndDirectoryIsNull(user);
    }

    @Override
    public List<File> getFilesByDirectory(Directory directory) {
        return fileRepository.findAllByDirectory(directory);
    }

    @Override
    public void shareFileWithUser(File file, User owner, User sharedWithUser, String versionNumber) {
        String versionPath = String.format("%s/versions/%s/%s", file.getPath(), versionNumber, file.getFileName());
        String sasUrl = azureBlobSASTokenGenerator.getBlobUrl(versionPath);

        // Versiyon bazlı paylaşımı kontrol et
        Optional<FileShare> existingShare = fileShareRepository.findByFileAndSharedWithUserAndVersion(file, sharedWithUser, versionNumber);
        FileShare fileShare;
        if (existingShare.isPresent()) {
            fileShare = existingShare.get();
            fileShare.setSasUrl(sasUrl);
        } else {
            fileShare = new FileShare();
            fileShare.setFile(file);
            fileShare.setOwner(owner);
            fileShare.setSharedWithUser(sharedWithUser);
            fileShare.setSasUrl(sasUrl);
            fileShare.setVersion(versionNumber);
        }

        fileShareRepository.save(fileShare);
        owner.getContacts().add(sharedWithUser);
        userRepository.save(owner);
    }


    @Override
    public List<FileDto> getUserFilesInRootDirectoryAsDto(User user) {
        return getUserFilesInRootDirectory(user).stream().map(fileMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<FileDto> getFilesByDirectoryAsDto(Directory directory) {
        return getFilesByDirectory(directory).stream().map(fileMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<FileDto> getFilesByUsername(String username) {
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<File> files = getFilesByUser(user);
        return files.stream().map(fileMapper::toDto).toList();
    }

    @Override
    public List<FileShareDto> getSharedFilesByUsername(String username) {
        User user = userManagementService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<FileShare> fileShares = fileShareRepository.findAllBySharedWithUser(user);
        return fileShares.stream().map(fileShareMapper::toDto).toList();
    }

    @Override
    public long getTotalStorage(String username) {
        return 10L * 1024 * 1024 * 1024;
    }

    @Override
    public long getUsedStorage(String username) {
        List<FileVersion> allVersions = fileVersionRepository.findAllByUsername(username);
        return allVersions.stream().mapToLong(FileVersion::getSize).sum();
    }
    @Override
    public List<FileDto> getLastUploadedFiles(String username, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<FileVersion> versions = fileVersionRepository.findTopVersionsByUsername(username, pageable);

        return versions.stream()
                .map(version -> {
                    File file = version.getFile();
                    String ownerUsername = file.getUser().getUsername();
                    Long directoryId = file.getDirectory() != null ? file.getDirectory().getId() : null;

                    FileVersionDto versionDto = new FileVersionDto(
                            version.getId(),
                            version.getVersionNumber(),
                            version.getDeltaPath(),
                            version.getHash(),
                            version.getTimestamp(),
                            version.getSize()
                    );

                    return new FileDto(
                            file.getId(),
                            file.getFileName(),
                            file.getPath(),
                            ownerUsername,
                            directoryId,
                            List.of(versionDto),
                            List.of(),
                            null
                    );

                })
                .collect(Collectors.toList());
    }
    @Override
    public List<FileVersionDto> getLastUploadedFileVersions(String username, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<FileVersion> versions = fileVersionRepository.findTopVersionsByUsername(username, pageable);

        return versions.stream()
                .map(version -> new FileVersionDto(
                        version.getId(),
                        version.getVersionNumber(),
                        version.getDeltaPath(),
                        version.getHash(),
                        version.getTimestamp(),
                        version.getSize()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<FileVersionDto> getLastAccessedFileVersions(String username, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<FileVersion> versions = fileVersionRepository.findLastAccessedByUsername(username, pageable);

        return versions.stream()
                .map(version -> new FileVersionDto(
                        version.getId(),
                        version.getVersionNumber(),
                        version.getDeltaPath(),
                        version.getHash(),
                        version.getTimestamp(),
                        version.getSize()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String getFileNameByVersionId(Long versionId) {
        return fileVersionRepository.findById(versionId)
                .map(FileVersion::getFile)
                .map(File::getFileName)
                .orElseThrow(() -> new IllegalArgumentException("File version not found for ID: " + versionId));
    }

}
