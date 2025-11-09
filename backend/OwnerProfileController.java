package com.hungerexpress.owner;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.restaurant.Cuisine;
import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerProfileController {

    private final UserRepository users;
    private final RestaurantRepository restaurants;
    private final OwnerProfileRepository ownerProfiles;

    // DTOs
    public record OwnerProfileResponse(
        Long id,
        Long userId,
        String email,
        String fullName,
        String phone,
        String profilePictureUrl,
        String address,
        String city,
        String state,
        String postalCode,
        String businessName,
        String businessEmail,
        String gstNumber,
        String panNumber,
        Boolean profileCompleted,
        Boolean verified,
        List<RestaurantSummary> restaurants
    ) {}

    public record RestaurantSummary(
        Long id,
        String name,
        String description,
        String imageUrl,
        String logoUrl,
        String address,
        String city,
        String phone,
        String cuisine,
        Double rating,
        Boolean approved,
        Boolean active,
        Boolean profileCompleted,
        Boolean isOnline
    ) {}

    public record UpdateProfileRequest(
        String fullName,
        String phone,
        String profilePictureUrl,
        String address,
        String city,
        String state,
        String postalCode,
        String businessName,
        String businessEmail,
        String gstNumber,
        String panNumber
    ) {}

    public record CreateRestaurantRequest(
        String name,
        String description,
        String address,
        String city,
        String state,
        String postalCode,
        String phone,
        String email,
        String cuisine,
        String gstNumber,
        String fssaiLicense,
        String businessPan,
        String openingTime,
        String closingTime,
        String specialty,
        String tags
    ) {}

    public record UpdateRestaurantRequest(
        String name,
        String description,
        String address,
        String city,
        String state,
        String postalCode,
        String phone,
        String email,
        String imageUrl,
        String logoUrl,
        String coverImageUrl,
        String cuisine,
        BigDecimal deliveryFee,
        BigDecimal minOrderAmount,
        Integer avgPreparationTime,
        String gstNumber,
        String fssaiLicense,
        String businessPan,
        String openingTime,
        String closingTime,
        String specialty,
        String tags,
        String websiteUrl,
        String latitude,
        String longitude
    ) {}

    /**
     * Get owner profile with all restaurants
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<OwnerProfileResponse> getProfile() {
        try {
            System.out.println("[OwnerProfileController] GET /api/owner/profile called");
            String email = CurrentUser.email();
            System.out.println("[OwnerProfileController] Current user email: " + email);
            if (email == null) {
                System.out.println("[OwnerProfileController] Email is null - returning 401");
                return ResponseEntity.status(401).build();
            }

            User user = users.findByEmail(email).orElse(null);
            if (user == null) {
                System.out.println("[OwnerProfileController] User not found for email: " + email);
                return ResponseEntity.status(404).build();
            }
            System.out.println("[OwnerProfileController] User found: " + user.getId());

            // Get or create owner profile
            OwnerProfile ownerProfile = ownerProfiles.findByUserId(user.getId())
                .orElseGet(() -> {
                    System.out.println("[OwnerProfileController] Creating new owner profile for user: " + user.getId());
                    OwnerProfile newProfile = OwnerProfile.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .profileCompleted(false)
                        .verified(false)
                        .build();
                    return ownerProfiles.save(newProfile);
                });
            System.out.println("[OwnerProfileController] Owner profile loaded: " + ownerProfile.getId());

            List<Restaurant> ownerRestaurants = restaurants.findByOwnerId(user.getId());
            System.out.println("[OwnerProfileController] Found " + ownerRestaurants.size() + " restaurants");
            
            List<RestaurantSummary> restaurantSummaries = ownerRestaurants.stream()
                .map(r -> new RestaurantSummary(
                    r.getId(),
                    r.getName(),
                    r.getDescription(),
                    r.getImageUrl(),
                    r.getLogoUrl(),
                    r.getAddress(),
                    r.getCity(),
                    r.getPhone(),
                    r.getCuisine() != null ? r.getCuisine().name() : null,
                    r.getRating(),
                    r.getApproved(),
                    r.getActive(),
                    r.getProfileCompleted(),
                    r.getIsOnline()
                ))
                .toList();

            OwnerProfileResponse response = new OwnerProfileResponse(
                ownerProfile.getId(),
                user.getId(),
                user.getEmail(),
                ownerProfile.getFullName(),
                ownerProfile.getPhone(),
                ownerProfile.getProfilePictureUrl(),
                ownerProfile.getAddress(),
                ownerProfile.getCity(),
                ownerProfile.getState(),
                ownerProfile.getPostalCode(),
                ownerProfile.getBusinessName(),
                ownerProfile.getBusinessEmail(),
                ownerProfile.getGstNumber(),
                ownerProfile.getPanNumber(),
                ownerProfile.getProfileCompleted(),
                ownerProfile.getVerified(),
                restaurantSummaries
            );

            System.out.println("[OwnerProfileController] Returning profile response successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[OwnerProfileController] ERROR in getProfile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Update owner profile
     */
    @PutMapping
    @Transactional
    public ResponseEntity<OwnerProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        // Get or create owner profile
        OwnerProfile ownerProfile = ownerProfiles.findByUserId(user.getId())
            .orElseGet(() -> {
                OwnerProfile newProfile = OwnerProfile.builder()
                    .userId(user.getId())
                    .build();
                return ownerProfiles.save(newProfile);
            });

        // Update owner profile
        ownerProfile.setFullName(request.fullName());
        ownerProfile.setPhone(request.phone());
        if (request.profilePictureUrl() != null && !request.profilePictureUrl().isEmpty()) {
            ownerProfile.setProfilePictureUrl(request.profilePictureUrl());
        }
        ownerProfile.setAddress(request.address());
        ownerProfile.setCity(request.city());
        ownerProfile.setState(request.state());
        ownerProfile.setPostalCode(request.postalCode());
        ownerProfile.setBusinessName(request.businessName());
        ownerProfile.setBusinessEmail(request.businessEmail());
        ownerProfile.setGstNumber(request.gstNumber());
        ownerProfile.setPanNumber(request.panNumber());
        ownerProfile.setProfileCompleted(true);
        ownerProfile.setUpdatedAt(Instant.now());

        ownerProfiles.save(ownerProfile);

        // Return updated profile
        return getProfile();
    }

    /**
     * Toggle restaurant online/offline status
     */
    @PostMapping("/restaurant/{restaurantId}/toggle-online")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleRestaurantOnlineStatus(@PathVariable Long restaurantId) {
        try {
            String email = CurrentUser.email();
            if (email == null) {
                return ResponseEntity.status(401).build();
            }

            User user = users.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).build();
            }

            Restaurant restaurant = restaurants.findById(restaurantId).orElse(null);
            if (restaurant == null) {
                return ResponseEntity.status(404).build();
            }

            // Verify ownership
            if (!restaurant.getOwnerId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            // Toggle online status
            boolean newStatus = !Boolean.TRUE.equals(restaurant.getIsOnline());
            restaurant.setIsOnline(newStatus);
            restaurant.setUpdatedAt(Instant.now());
            restaurants.save(restaurant);

            System.out.println("[OwnerProfileController] Restaurant " + restaurantId + " is now " + (newStatus ? "ONLINE" : "OFFLINE"));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "isOnline", newStatus,
                "message", newStatus ? "Restaurant is now accepting orders" : "Restaurant is now offline"
            ));
        } catch (Exception e) {
            System.err.println("[OwnerProfileController] Error toggling online status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get restaurant online status
     */
    @GetMapping("/restaurant/{restaurantId}/status")
    public ResponseEntity<Map<String, Object>> getRestaurantStatus(@PathVariable Long restaurantId) {
        try {
            String email = CurrentUser.email();
            if (email == null) {
                return ResponseEntity.status(401).build();
            }

            User user = users.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).build();
            }

            Restaurant restaurant = restaurants.findById(restaurantId).orElse(null);
            if (restaurant == null) {
                return ResponseEntity.status(404).build();
            }

            // Verify ownership
            if (!restaurant.getOwnerId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(Map.of(
                "isOnline", Boolean.TRUE.equals(restaurant.getIsOnline()),
                "restaurantId", restaurantId,
                "restaurantName", restaurant.getName()
            ));
        } catch (Exception e) {
            System.err.println("[OwnerProfileController] Error getting status: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Upload profile picture
     */
    @PostMapping("/upload-picture")
    @Transactional
    public ResponseEntity<Map<String, String>> uploadProfilePicture(@RequestParam("file") MultipartFile file) {
        try {
            String email = CurrentUser.email();
            if (email == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }

            User user = users.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Create uploads directory if it doesn't exist
            Path uploadDir = Paths.get("uploads/owners");
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "owner_" + user.getId() + "_" + UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update owner profile with file URL
            String fileUrl = "/uploads/owners/" + filename;
            OwnerProfile ownerProfile = ownerProfiles.findByUserId(user.getId())
                .orElseGet(() -> {
                    OwnerProfile newProfile = OwnerProfile.builder()
                        .userId(user.getId())
                        .build();
                    return ownerProfiles.save(newProfile);
                });
            
            ownerProfile.setProfilePictureUrl(fileUrl);
            ownerProfile.setUpdatedAt(Instant.now());
            ownerProfiles.save(ownerProfile);

            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (Exception e) {
            System.err.println("Failed to upload file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Create new restaurant for owner
     */
    @PostMapping("/restaurant")
    @Transactional
    public ResponseEntity<RestaurantSummary> createRestaurant(@RequestBody CreateRestaurantRequest request) {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        System.out.println("[OwnerProfileController] Creating restaurant for user: " + user.getId());
        System.out.println("[OwnerProfileController] Restaurant name: " + request.name());
        
        // Create new restaurant
        Restaurant restaurant = Restaurant.builder()
            .ownerId(user.getId())
            .name(request.name())
            .description(request.description())
            .address(request.address())
            .city(request.city())
            .state(request.state())
            .postalCode(request.postalCode())
            .phone(request.phone())
            .email(request.email())
            .cuisine(request.cuisine() != null ? Cuisine.valueOf(request.cuisine()) : Cuisine.INDIAN)
            .gstNumber(request.gstNumber())
            .fssaiLicense(request.fssaiLicense())
            .businessPan(request.businessPan())
            .openingTime(request.openingTime())
            .closingTime(request.closingTime())
            .specialty(request.specialty())
            .tags(request.tags())
            .approved(true)  // Auto-approve for development
            .active(true)
            .profileCompleted(true)
            .isOnline(false)  // Offline by default
            .rating(0.0)
            .deliveryFee(BigDecimal.valueOf(50))  // Default delivery fee
            .createdAt(Instant.now())
            .build();

        Restaurant saved = restaurants.save(restaurant);
        System.out.println("[OwnerProfileController] Restaurant created with ID: " + saved.getId());

        RestaurantSummary summary = new RestaurantSummary(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getImageUrl(),
            saved.getLogoUrl(),
            saved.getAddress(),
            saved.getCity(),
            saved.getPhone(),
            saved.getCuisine() != null ? saved.getCuisine().name() : null,
            saved.getRating(),
            saved.getApproved(),
            saved.getActive(),
            saved.getProfileCompleted(),
            saved.getIsOnline()
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Update existing restaurant
     */
    @PutMapping("/restaurant/{id}")
    @Transactional
    public ResponseEntity<RestaurantSummary> updateRestaurant(
            @PathVariable Long id,
            @RequestBody UpdateRestaurantRequest request) {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        Restaurant restaurant = restaurants.findById(id).orElse(null);
        if (restaurant == null) {
            return ResponseEntity.status(404).build();
        }

        // Verify ownership
        if (!restaurant.getOwnerId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        // Update restaurant details
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setCity(request.city());
        restaurant.setState(request.state());
        restaurant.setPostalCode(request.postalCode());
        restaurant.setPhone(request.phone());
        restaurant.setEmail(request.email());
        restaurant.setImageUrl(request.imageUrl());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setCoverImageUrl(request.coverImageUrl());
        
        if (request.cuisine() != null) {
            restaurant.setCuisine(Cuisine.valueOf(request.cuisine()));
        }
        
        restaurant.setDeliveryFee(request.deliveryFee());
        restaurant.setMinOrderAmount(request.minOrderAmount());
        restaurant.setAvgPreparationTime(request.avgPreparationTime());
        restaurant.setGstNumber(request.gstNumber());
        restaurant.setFssaiLicense(request.fssaiLicense());
        restaurant.setBusinessPan(request.businessPan());
        restaurant.setOpeningTime(request.openingTime());
        restaurant.setClosingTime(request.closingTime());
        restaurant.setSpecialty(request.specialty());
        restaurant.setTags(request.tags());
        restaurant.setWebsiteUrl(request.websiteUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setUpdatedAt(Instant.now());
        restaurant.setProfileCompleted(true);

        Restaurant saved = restaurants.save(restaurant);

        RestaurantSummary summary = new RestaurantSummary(
            saved.getId(),
            saved.getName(),
            saved.getDescription(),
            saved.getImageUrl(),
            saved.getLogoUrl(),
            saved.getAddress(),
            saved.getCity(),
            saved.getPhone(),
            saved.getCuisine() != null ? saved.getCuisine().name() : null,
            saved.getRating(),
            saved.getApproved(),
            saved.getActive(),
            saved.getProfileCompleted(),
            saved.getIsOnline()
        );

        return ResponseEntity.ok(summary);
    }

    /**
     * Get all restaurants owned by current user
     */
    @GetMapping("/restaurants")
    @Transactional(readOnly = true)
    public ResponseEntity<List<RestaurantSummary>> getRestaurants() {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        List<Restaurant> ownerRestaurants = restaurants.findByOwnerId(user.getId());
        
        List<RestaurantSummary> summaries = ownerRestaurants.stream()
            .map(r -> new RestaurantSummary(
                r.getId(),
                r.getName(),
                r.getDescription(),
                r.getImageUrl(),
                r.getLogoUrl(),
                r.getAddress(),
                r.getCity(),
                r.getPhone(),
                r.getCuisine() != null ? r.getCuisine().name() : null,
                r.getRating(),
                r.getApproved(),
                r.getActive(),
                r.getProfileCompleted(),
                r.getIsOnline()
            ))
            .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get specific restaurant details
     */
    @GetMapping("/restaurant/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Restaurant> getRestaurant(@PathVariable Long id) {
        String email = CurrentUser.email();
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        Restaurant restaurant = restaurants.findById(id).orElse(null);
        if (restaurant == null) {
            return ResponseEntity.status(404).build();
        }

        // Verify ownership
        if (!restaurant.getOwnerId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(restaurant);
    }
}
