package com.example.securedrive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_versions")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(nullable = false)
    private String hash;

    @Column(nullable = false)
    private String versionNumber;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String deltaPath;

    @Column(nullable = false)
    private Long size;

    @Column
    private LocalDateTime lastAccessed;
}
