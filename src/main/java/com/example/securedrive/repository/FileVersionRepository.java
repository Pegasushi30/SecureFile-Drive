package com.example.securedrive.repository;

import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findAllByFile(File file);
    FileVersion findByFileAndVersionNumber(File file, String versionNumber);
    void deleteAllByFile(File file);
    List<FileVersion> findByFile_Directory_User_UsernameOrderByTimestampDesc(String username, Pageable pageable);
    List<FileVersion> findByFile_Directory_User_UsernameOrderByLastAccessedDesc(String username, Pageable pageable);
    List<FileVersion> findByFile_Directory_User_Username(String username);
}
