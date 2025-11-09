package com.hungerexpress.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;
    private String phone;
    
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;
    
    @Column(length = 300)
    private String address;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Builder.Default
    private Boolean enabled = true;
    
    @Builder.Default
    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(length = 20)
    private String role;
}
