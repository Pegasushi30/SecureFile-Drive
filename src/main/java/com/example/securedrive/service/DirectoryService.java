package com.example.securedrive.service;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.dto.DirectoryShareRequestDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.User;

import java.util.List;
import java.util.Optional;

public interface DirectoryService {
    List<Directory> getRootDirectories(User user);
    List<Directory> getSubDirectories(User user, Directory parentDirectory);
    void saveDirectory(Directory directory);
    Optional<Directory> findByIdAndUser(Long id, User user);
    void deleteDirectory(Directory directory);
    List<Directory> getUserDirectories(User user);
    Optional<Directory> getDirectoryById(Long id);
    List<DirectoryDto> getRootDirectoriesAsDto(User user);
    List<DirectoryDto> getSubDirectoriesAsDto(User user, Long parentDirectoryId);
    DirectoryDto getDirectoryByIdAsDto(Long id, User user);
    DirectoryDto createDirectory(DirectoryDto directoryDto);
    void deleteDirectoryById(Long id, User user);
    List<DirectoryDto> getDirectoriesByUsername(String username);
    void shareDirectory(DirectoryShareRequestDto dto, User owner);
    void revokeDirectoryShare(Long directoryId, String sharedWithEmail, User owner);
    void revokeDirectoryShareRecursive(Directory directory, User sharedWithUser) ;
    List<DirectoryShareDto> getMySharedDirectories(User owner);
    List<DirectoryShareDto> getSharedDirectories(User user);
    List<DirectoryShareDto> getMySharedDirectoriesForDirectory(User owner, Long directoryId);
}



