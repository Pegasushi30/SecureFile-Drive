package com.example.securedrive.repository;

import com.example.securedrive.model.File;
import com.example.securedrive.model.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    List<FileVersion> findAllByFile(File file);
    FileVersion findByFileAndVersionNumber(File file, String versionNumber);
    @Modifying
    @Query("DELETE FROM FileVersion fv WHERE fv.file = :file")
    void deleteAllByFile(File file);
}
