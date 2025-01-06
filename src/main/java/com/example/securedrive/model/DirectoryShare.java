package com.example.securedrive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "directory_share")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DirectoryShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "directory_id", nullable = false)
    private Directory directory; // Paylaşılan dizin

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Dizin sahibi

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_with_user_id", nullable = false)
    private User sharedWithUser; // Paylaşılan kullanıcı

    @Column(length = 2000)
    private String sharedPath; // İsteğe bağlı URL veya tanımlayıcı bir yol

    @Column(nullable = false)
    private LocalDateTime sharedAt = LocalDateTime.now(); // Paylaşım zamanı
}
