package com.example.securedrive.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "file_share")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FileShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private File file;

    @ManyToOne
    private User owner;

    @ManyToOne
    private User sharedWithUser;

    @Column(length = 2000)
    private String sasUrl;

    private String version;
}
