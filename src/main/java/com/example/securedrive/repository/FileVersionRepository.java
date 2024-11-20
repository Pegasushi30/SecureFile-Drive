package com.example.securedrive.repository;

import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findAllByFile(File file);
    FileVersion findByFileAndVersionNumber(File file, String versionNumber);
}
