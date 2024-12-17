package com.example.securedrive.mapper;

import com.example.securedrive.dto.DirectoryDto;
import com.example.securedrive.model.Directory;
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
                        : Collections.emptyList() // Boş liste atandı
        );
    }

    public Directory toEntity(DirectoryDto dto) {
        if (dto == null) return null;
        Directory directory = new Directory();
        directory.setName(dto.name());
        // parentDirectory ve user daha sonra service'te set edilecek
        return directory;
    }
}
