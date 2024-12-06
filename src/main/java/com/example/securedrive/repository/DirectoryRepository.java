package com.example.securedrive.repository;

import com.example.securedrive.model.Directory;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends JpaRepository<Directory, Long> {
    List<Directory> findAllByUser(User user);
    List<Directory> findAllByUserAndParentDirectory(User user, Directory parentDirectory);
    List<Directory> findAllByUserAndParentDirectoryIsNull(User user);
    Optional<Directory> findByIdAndUser(Long id, User user);
}
