package com.hungerexpress.config;

import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import com.hungerexpress.restaurant.Cuisine;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RestaurantRepository restaurants;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        System.out.println("[DataInitializer] Initializing database with baseline data...");

        // Create test accounts for dev-login
        createTestAccountIfNotExists("admin", "admin", "Admin", "ADMIN");
        createTestAccountIfNotExists("badalkusingh8@gmail.com", "admin123", "Admin User", "ADMIN");
        User ownerUser = createTestAccountIfNotExists("owner@test.com", "owner123", "Restaurant Owner", "OWNER");
        createTestAccountIfNotExists("customer@test.com", "customer123", "Test Customer", "CUSTOMER");
        createTestAccountIfNotExists("agent@test.com", "agent123", "Delivery Agent", "AGENT");
        
        // Create multiple sample restaurants for test owners
        if (restaurants.count() == 0) {
            System.out.println("[DataInitializer] Creating sample restaurants...");
            
            // Create additional owner accounts
            User owner2 = createTestAccountIfNotExists("owner2@test.com", "owner123", "Owner Two", "OWNER");
            User owner3 = createTestAccountIfNotExists("owner3@test.com", "owner123", "Owner Three", "OWNER");
            User owner4 = createTestAccountIfNotExists("owner4@test.com", "owner123", "Owner Four", "OWNER");
            User owner5 = createTestAccountIfNotExists("owner5@test.com", "owner123", "Owner Five", "OWNER");
            
            // Create restaurants for owners
            if (ownerUser != null) {
                Restaurant r1 = Restaurant.builder()
                        .ownerId(ownerUser.getId())
                        .name("Spice Garden")
                        .description("Authentic Indian cuisine with traditional flavors")
                        .cuisine(Cuisine.INDIAN)
                        .address("123 MG Road")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .phone("9876543210")
                        .rating(4.5)
                        .deliveryFee(BigDecimal.valueOf(40))
                        .approved(true)
                        .active(true)
                        .profileCompleted(true)
                        .imageUrl("https://placehold.co/400x300/FF6B6B/FFFFFF?text=Spice+Garden")
                        .build();
                restaurants.save(r1);
            }
            
            if (owner2 != null) {
                Restaurant r2 = Restaurant.builder()
                        .ownerId(owner2.getId())
                        .name("Pizza Paradise")
                        .description("Wood-fired pizzas and Italian delights")
                        .cuisine(Cuisine.ITALIAN)
                        .address("456 Park Street")
                        .city("Delhi")
                        .state("Delhi")
                        .phone("9876543211")
                        .rating(4.3)
                        .deliveryFee(BigDecimal.valueOf(50))
                        .approved(true)
                        .active(true)
                        .profileCompleted(true)
                        .imageUrl("https://placehold.co/400x300/4ECDC4/FFFFFF?text=Pizza+Paradise")
                        .build();
                restaurants.save(r2);
            }
            
            if (owner3 != null) {
                Restaurant r3 = Restaurant.builder()
                        .ownerId(owner3.getId())
                        .name("Dragon Wok")
                        .description("Authentic Chinese cuisine and dim sum")
                        .cuisine(Cuisine.CHINESE)
                        .address("789 China Town")
                        .city("Bangalore")
                        .state("Karnataka")
                        .phone("9876543212")
                        .rating(4.7)
                        .deliveryFee(BigDecimal.valueOf(45))
                        .approved(true)
                        .active(true)
                        .profileCompleted(true)
                        .imageUrl("https://placehold.co/400x300/FFE66D/000000?text=Dragon+Wok")
                        .build();
                restaurants.save(r3);
            }
            
            if (owner4 != null) {
                Restaurant r4 = Restaurant.builder()
                        .ownerId(owner4.getId())
                        .name("Burger Hub")
                        .description("Gourmet burgers and American fast food")
                        .cuisine(Cuisine.AMERICAN)
                        .address("321 Mall Road")
                        .city("Chennai")
                        .state("Tamil Nadu")
                        .phone("9876543213")
                        .rating(4.2)
                        .deliveryFee(BigDecimal.valueOf(35))
                        .approved(true)
                        .active(true)
                        .profileCompleted(true)
                        .imageUrl("https://placehold.co/400x300/95E1D3/000000?text=Burger+Hub")
                        .build();
                restaurants.save(r4);
            }
            
            if (owner5 != null) {
                Restaurant r5 = Restaurant.builder()
                        .ownerId(owner5.getId())
                        .name("Thai Spice")
                        .description("Traditional Thai flavors and curries")
                        .cuisine(Cuisine.THAI)
                        .address("654 Beach Road")
                        .city("Goa")
                        .state("Goa")
                        .phone("9876543214")
                        .rating(4.6)
                        .deliveryFee(BigDecimal.valueOf(55))
                        .approved(true)
                        .active(true)
                        .profileCompleted(true)
                        .imageUrl("https://placehold.co/400x300/F38181/FFFFFF?text=Thai+Spice")
                        .build();
                restaurants.save(r5);
            }
            
            System.out.println("[DataInitializer] ✅ Created 5 sample restaurants with 5 owners");
        }
        
        System.out.println("[DataInitializer] ✅ Database initialization complete!");
        System.out.println("[DataInitializer] Test accounts ready for dev-login:");
        System.out.println("[DataInitializer]   - Admin (Simple): admin / admin");
        System.out.println("[DataInitializer]   - Admin: badalkusingh8@gmail.com / admin123");
        System.out.println("[DataInitializer]   - Owner: owner@test.com / owner123");
        System.out.println("[DataInitializer]   - Customer: customer@test.com / customer123");
        System.out.println("[DataInitializer]   - Agent: agent@test.com / agent123");
    }
    
    private User createTestAccountIfNotExists(String email, String password, String fullName, String roleName) {
        return users.findByEmail(email).orElseGet(() -> {
            System.out.println("[DataInitializer] Creating test account: " + email + " (Role: " + roleName + ")");
            User u = new User();
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPassword(encoder.encode(password));
            u.setEnabled(true);
            u.setRole(roleName);
            
            User saved = users.save(u);
            System.out.println("[DataInitializer] ✅ Created user: " + email + " with role: " + roleName);
            return saved;
        });
    }
}
