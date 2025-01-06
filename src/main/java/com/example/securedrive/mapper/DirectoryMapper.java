package com.example.securedrive.mapper;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.dto.DirectoryShareDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.DirectoryShare;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class DirectoryMapper {

    public DirectoryDto toDto(Directory directory) {
        return new DirectoryDto(
                directory.getId(),
                directory.getName(),
                directory.getParentDirectory() != null ? directory.getParentDirectory().getId() : null,
                directory.getUser() != null ? directory.getUser().getUsername() : null,
                directory.getSubDirectories() != null
                        ? directory.getSubDirectories().stream().map(this::toDto).collect(Collectors.toList())
                        : Collections.emptyList(),
                directory.getDirectoryShares() != null
                        ? directory.getDirectoryShares().stream().map(this::toShareDto).collect(Collectors.toList())
                        : Collections.emptyList()
        );
    }

    private DirectoryShareDto toShareDto(DirectoryShare share) {
        return new DirectoryShareDto(
                share.getId(),
                share.getDirectory().getId(),                 // directoryId
                share.getDirectory().getName(),               // directoryName
                share.getSharedWithUser().getEmail(),         // sharedWithUserEmail
                share.getOwner().getEmail(),                  // ownerEmail
                share.getSharedPath()
        );
    }
}
