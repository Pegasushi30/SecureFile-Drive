package com.example.securedrive.repository;

import com.example.securedrive.model.File;
import com.example.securedrive.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findAllByUser(User user);
    File findByFileNameAndUser(String fileName, User user);
}
