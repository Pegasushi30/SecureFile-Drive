package com.example.securedrive.repository;

import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findAllByUser(User user);
    File findByFileNameAndUser(String fileName, User user);
}
