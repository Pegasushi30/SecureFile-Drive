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

    @Query("SELECT fv FROM FileVersion fv JOIN fv.file f JOIN f.user u " +
            "WHERE u.username = :username " +
            "ORDER BY fv.timestamp DESC")
    List<FileVersion> findTopVersionsByUsername(@org.springframework.data.repository.query.Param("username") String username, org.springframework.data.domain.Pageable pageable);
    @Query("SELECT fv FROM FileVersion fv JOIN fv.file f JOIN f.user u " +
            "WHERE u.username = :username " +
            "ORDER BY fv.lastAccessed DESC")
    List<FileVersion> findLastAccessedByUsername(@org.springframework.data.repository.query.Param("username") String username, org.springframework.data.domain.Pageable pageable);
    @Query("SELECT fv FROM FileVersion fv JOIN fv.file f JOIN f.user u " +
            "WHERE u.username = :username")
    List<FileVersion> findAllByUsername(@org.springframework.data.repository.query.Param("username") String username);

}
