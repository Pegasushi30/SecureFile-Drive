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

    // Dosya ilişkisi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(nullable = false)
    private String versionNumber; // Versiyon numarası

    @Column(nullable = false)
    private LocalDateTime timestamp; // Versiyon oluşturulma zamanı

    private String deltaPath; // Delta dosyasının yolu (v1 için null)
}
