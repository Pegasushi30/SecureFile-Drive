package com.example.securedrive.mapper;

import com.example.securedrive.dto.FileShareDto;
import com.example.securedrive.model.Directory;
import com.example.securedrive.model.FileShare;
import org.springframework.stereotype.Component;

@Component
public class FileShareMapper {

    public FileShareDto toDto(FileShare fileShare) {
        if (fileShare == null) return null;

        String fileName = fileShare.getFile() != null ? fileShare.getFile().getFileName() : "Unknown file";
        String ownerEmail = fileShare.getOwner() != null ? fileShare.getOwner().getEmail() : "Unknown owner";

        Directory directory = fileShare.getFile() != null ? fileShare.getFile().getDirectory() : null;
        String directoryName = directory != null ? directory.getName() : "Main Directory";
        String directoryPath = directory != null ? buildDirectoryPath(directory) : "/";

        return new FileShareDto(
                fileShare.getId(),
                fileShare.getSharedWithUser().getEmail(),
                fileShare.getVersion(),
                fileShare.getSasUrl(),
                fileName,
                ownerEmail,
                directoryName,
                directoryPath
        );
    }

    private String buildDirectoryPath(Directory directory) {
        if (directory == null) return "/";
        StringBuilder pathBuilder = new StringBuilder();
        buildPath(directory, pathBuilder);
        return pathBuilder.toString();
    }

    private void buildPath(Directory directory, StringBuilder pathBuilder) {
        if (directory.getParentDirectory() != null) {
            buildPath(directory.getParentDirectory(), pathBuilder);
        }
        pathBuilder.append("/").append(directory.getName());
    }
}
