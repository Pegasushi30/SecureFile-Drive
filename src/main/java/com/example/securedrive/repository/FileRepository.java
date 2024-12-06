package com.example.securedrive.repository;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findAllByUser(User user);
    Optional<File> findByIdAndUser(Long id, User user);
    Optional<File> findByUserAndFileNameAndDirectory(User user, String fileName, Directory directory);
    List<File> findAllByDirectory(Directory directory);
    Optional<File> findByUserAndFileNameAndDirectoryIsNull(User user, String fileName);
    List<File> findAllByUserAndDirectoryIsNull(User user);
}

