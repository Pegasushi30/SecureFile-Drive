package com.example.securedrive.repository;

import com.example.securedrive.dto.DirectoryShareEmailDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.DirectoryShare;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryShareRepository extends JpaRepository<DirectoryShare, Long> {
    List<DirectoryShare> findBySharedWithUser(User sharedWithUser);
    Optional<DirectoryShare> findByDirectoryAndSharedWithUser(Directory directory, User sharedWithUser);
    List<DirectoryShare> findBySharedWithUserId(Long userId);

    // Yeni metod: Kullanıcı sahibi olduğu paylaşımları bulur
    List<DirectoryShare> findByOwner(User owner);

    // Find directories shared by a specific owner
    List<DirectoryShare> findByDirectoryAndOwner(Directory directory, User owner);
}
