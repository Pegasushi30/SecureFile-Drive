package com.example.securedrive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "files")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String path;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL)
    private Set<FileShare> fileShares = new HashSet<>();

    // Dosyanın sahibi kullanıcı
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // Buradaki nullable=false, user_id'nin boş olamayacağını belirtir
    private User user;

    // Dosyanın versiyonları
    @OneToMany(mappedBy = "file", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<FileVersion> versions; // Bu alanda, dosyaya ait versiyonları tutacağız
}
