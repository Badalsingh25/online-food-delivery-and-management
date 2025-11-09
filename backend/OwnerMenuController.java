package com.hungerexpress.menu;

import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/owner/restaurants/{rid}/menu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class OwnerMenuController {
    private final RestaurantRepository restaurants;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;
    private final UserRepository users;
    private final AdminNotificationService adminNotificationService;

    private Restaurant rest(Long id){ return restaurants.findById(id).orElseThrow(); }

    // GET all categories and items for owner (includes pending/rejected)
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<MenuCategory>> getOwnerMenu(@PathVariable Long rid){
        Restaurant r = rest(rid);
        List<MenuCategory> cats = categories.findByRestaurantOrderByPositionAsc(r);
        // Show ALL items to owner (not just approved)
        cats.forEach(c -> c.setItems(items.findByCategoryIdOrderByIdAsc(c.getId())));
        
        // Also include items with no category
        List<MenuItem> uncategorizedItems = items.findByRestaurant(r).stream()
            .filter(item -> item.getCategory() == null)
            .toList();
        
        if (!uncategorizedItems.isEmpty()) {
            MenuCategory uncategorized = new MenuCategory();
            uncategorized.setId(0L);
            uncategorized.setName("Uncategorized");
            uncategorized.setPosition(999);
            uncategorized.setItems(uncategorizedItems);
            cats.add(uncategorized);
        }
        
        System.out.println("[OwnerMenuController] GET menu for restaurant " + rid + ": " + cats.size() + " categories");
        return ResponseEntity.ok(cats);
    }

    // Categories
    @PostMapping("/categories")
    public ResponseEntity<MenuCategory> createCategory(@PathVariable Long rid, @RequestBody MenuCategory payload){
        var r = rest(rid);
        payload.setId(null);
        payload.setRestaurant(r);
        MenuCategory saved = categories.save(payload);
        return ResponseEntity.created(URI.create("/api/owner/restaurants/"+rid+"/menu/categories/"+saved.getId())).body(saved);
    }

    @PutMapping("/categories/{cid}")
    public ResponseEntity<MenuCategory> updateCategory(@PathVariable Long rid, @PathVariable Long cid, @RequestBody MenuCategory payload){
        MenuCategory c = categories.findById(cid).orElseThrow();
        c.setName(payload.getName());
        c.setPosition(payload.getPosition());
        return ResponseEntity.ok(categories.save(c));
    }

    @DeleteMapping("/categories/{cid}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long rid, @PathVariable Long cid){
        categories.deleteById(cid);
        return ResponseEntity.noContent().build();
    }

    // Items
    @PostMapping("/items")
    @Transactional
    public ResponseEntity<MenuItem> createItem(@PathVariable Long rid, @RequestBody MenuItem payload){
        var r = rest(rid);
        payload.setId(null);
        payload.setRestaurant(r);
        
        // Validate and set category - MUST belong to same restaurant
        MenuCategory validCategory = null;
        if (payload.getCategory()!=null && payload.getCategory().getId()!=null){
            MenuCategory cat = categories.findById(payload.getCategory().getId()).orElse(null);
            // Verify category belongs to this restaurant
            if (cat != null && cat.getRestaurant() != null && cat.getRestaurant().getId().equals(rid)) {
                validCategory = cat;
            } else {
                System.err.println("[OwnerMenuController] ⚠️ Category " + payload.getCategory().getId() + " does not belong to restaurant " + rid);
            }
        }
        
        // Auto-assign first category if none provided or invalid
        if (validCategory == null) {
            List<MenuCategory> restaurantCategories = categories.findByRestaurantOrderByPositionAsc(r);
            if (!restaurantCategories.isEmpty()) {
                validCategory = restaurantCategories.get(0);
                System.out.println("[OwnerMenuController] ✅ Auto-assigned item to category: " + validCategory.getName());
            }
        }
        payload.setCategory(validCategory);
        
        // Set approval workflow fields - Require admin approval
        payload.setApprovalStatus(MenuItem.ApprovalStatus.PENDING);
        payload.setSubmittedAt(Instant.now());
        
        // Get current user ID (owner who submitted)
        String email = CurrentUser.email();
        if (email != null) {
            users.findByEmail(email).ifPresent(u -> {
                payload.setSubmittedBy(u.getId());
            });
        }
        
        MenuItem saved = items.save(payload);
        
        System.out.println("[OwnerMenuController] Menu item submitted for approval: " + saved.getName() + " for restaurant: " + r.getName());
        
        // Send notification to admin (don't fail if notification fails)
        try {
            adminNotificationService.notifyNewItemSubmitted(saved, r);
        } catch (Exception e) {
            // Log but don't fail the request if notification fails
            System.err.println("Failed to send notification: " + e.getMessage());
        }
        
        return ResponseEntity.created(URI.create("/api/owner/restaurants/"+rid+"/menu/items/"+saved.getId())).body(saved);
    }

    @PutMapping("/items/{iid}")
    @Transactional
    public ResponseEntity<MenuItem> updateItem(@PathVariable Long rid, @PathVariable Long iid, @RequestBody MenuItem payload){
        MenuItem m = items.findById(iid).orElseThrow();
        
        // Check if significant changes require re-approval (safe null handling)
        boolean nameChanged = payload.getName() != null && !payload.getName().equals(m.getName());
        boolean priceChanged = payload.getPrice() != null && !payload.getPrice().equals(m.getPrice());
        boolean descChanged = false;
        if (payload.getDescription() != null && m.getDescription() != null) {
            descChanged = !payload.getDescription().equals(m.getDescription());
        } else if (payload.getDescription() != null || m.getDescription() != null) {
            descChanged = true;
        }
        
        boolean requiresReApproval = nameChanged || priceChanged || descChanged;
        
        // Update fields
        if (payload.getName() != null) m.setName(payload.getName());
        if (payload.getDescription() != null) m.setDescription(payload.getDescription());
        if (payload.getPrice() != null) m.setPrice(payload.getPrice());
        if (payload.getImageUrl() != null) m.setImageUrl(payload.getImageUrl());
        if (payload.getAvailable() != null) m.setAvailable(payload.getAvailable());
        
        // Validate and update category - MUST belong to same restaurant
        MenuCategory validCategory = m.getCategory(); // Keep existing if not specified
        if (payload.getCategory() != null && payload.getCategory().getId() != null){
            MenuCategory cat = categories.findById(payload.getCategory().getId()).orElse(null);
            // Verify category belongs to this restaurant
            if (cat != null && cat.getRestaurant() != null && cat.getRestaurant().getId().equals(rid)) {
                validCategory = cat;
            } else {
                System.err.println("[OwnerMenuController] ⚠️ Category " + payload.getCategory().getId() + " does not belong to restaurant " + rid);
            }
        }
        
        // Auto-fix if current category is wrong or missing
        if (validCategory == null || validCategory.getRestaurant() == null || !validCategory.getRestaurant().getId().equals(rid)) {
            List<MenuCategory> restaurantCategories = categories.findByRestaurantOrderByPositionAsc(m.getRestaurant());
            if (!restaurantCategories.isEmpty()) {
                validCategory = restaurantCategories.get(0);
                System.out.println("[OwnerMenuController] ✅ Auto-fixed item category to: " + validCategory.getName());
            }
        }
        m.setCategory(validCategory);
        
        // If item was approved and significant changes made, reset to PENDING
        if (requiresReApproval && m.getApprovalStatus() == MenuItem.ApprovalStatus.APPROVED) {
            m.setApprovalStatus(MenuItem.ApprovalStatus.PENDING);
            m.setSubmittedAt(Instant.now());
            m.setApprovedAt(null);
            m.setApprovedBy(null);
            
            MenuItem saved = items.save(m);
            try {
                adminNotificationService.notifyItemUpdated(saved, m.getRestaurant());
            } catch (Exception e) {
                // Log but don't fail the request if notification fails
                System.err.println("Failed to send notification: " + e.getMessage());
            }
            return ResponseEntity.ok(saved);
        }
        
        return ResponseEntity.ok(items.save(m));
    }

    @DeleteMapping("/items/{iid}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long rid, @PathVariable Long iid){
        items.deleteById(iid);
        return ResponseEntity.noContent().build();
    }
}
