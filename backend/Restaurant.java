package com.hungerexpress.restaurant;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "restaurant")
@Getter
 @Setter
@NoArgsConstructor
 @AllArgsConstructor
  @Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owner relationship
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Basic Information
    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    @Column(name = "cover_image_url", length = 300)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Cuisine cuisine;

    @Column(nullable = false)
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    // Location Details
    @Column(nullable = false, length = 200)
    private String address;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 50)
    private String country;

    @Column(length = 50)
    private String latitude;

    @Column(length = 50)
    private String longitude;

    // Contact Information
    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "website_url", length = 200)
    private String websiteUrl;

    // Business Details
    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "fssai_license", length = 50)
    private String fssaiLicense;

    @Column(name = "business_pan", length = 20)
    private String businessPan;

    // Operating Hours
    @Column(name = "opening_time", length = 10)
    private String openingTime;  // e.g., "09:00"

    @Column(name = "closing_time", length = 10)
    private String closingTime;  // e.g., "22:00"

    @Column(name = "is_open_now")
    @Builder.Default
    private Boolean isOpenNow = true;

    // Additional Info
    @Column(name = "min_order_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "avg_preparation_time")
    @Builder.Default
    private Integer avgPreparationTime = 30; // in minutes

    @Column(name = "specialty", length = 200)
    private String specialty; // e.g., "Famous for Biryani"

    @Column(name = "tags", length = 500)
    private String tags; // Comma-separated: "veg,non-veg,fast-food"

    // Status flags
    @Builder.Default
    @Column(nullable = false)
    private Boolean approved = false;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    @Builder.Default
    @Column(name = "is_online")
    private Boolean isOnline = false;  // Restaurant accepting orders right now

    @Builder.Default
    @Column(name = "profile_completed")
    private Boolean profileCompleted = false;

    // Timestamps
    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
