package com.example.securedrive.repository;


import com.example.securedrive.model.Directory;
import com.example.securedrive.model.DirectoryShare;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectoryShareRepository extends JpaRepository<DirectoryShare, Long> {
    Optional<DirectoryShare> findByDirectoryAndSharedWithUser(Directory directory, User sharedWithUser);
    List<DirectoryShare> findBySharedWithUserId(Long userId);
    List<DirectoryShare> findByOwner(User owner);
    List<DirectoryShare> findByDirectoryAndOwner(Directory directory, User owner);
}
