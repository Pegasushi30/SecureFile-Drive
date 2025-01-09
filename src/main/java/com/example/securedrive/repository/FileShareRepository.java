package com.example.securedrive.repository;

import com.example.securedrive.model.FileShare;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, Long> {
    List<FileShare> findAllBySharedWithUser(User sharedWithUser);
    Optional<FileShare> findByFileAndSharedWithUserAndVersion(File file, User sharedWithUser, String version);
    List<FileShare> findByFileId(Long fileId);
}
