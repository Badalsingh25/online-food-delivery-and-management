package com.hungerexpress.owner;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "owner_profile")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OwnerProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    // Personal Information
    @Column(name = "full_name", length = 120)
    private String fullName;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;
    
    // Address Information
    @Column(name = "address", length = 300)
    private String address;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Column(name = "country", length = 50)
    private String country;
    
    // Business Information (optional)
    @Column(name = "business_name", length = 150)
    private String businessName;
    
    @Column(name = "business_email", length = 150)
    private String businessEmail;
    
    @Column(name = "gst_number", length = 50)
    private String gstNumber;
    
    @Column(name = "pan_number", length = 20)
    private String panNumber;
    
    // Bank Details (optional)
    @Column(name = "bank_name", length = 100)
    private String bankName;
    
    @Column(name = "account_number", length = 50)
    private String accountNumber;
    
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;
    
    @Column(name = "account_holder_name", length = 120)
    private String accountHolderName;
    
    // Status
    @Builder.Default
    @Column(name = "profile_completed", nullable = false)
    private Boolean profileCompleted = false;
    
    @Builder.Default
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    // Timestamps
    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
