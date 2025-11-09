package com.hungerexpress.menu;

import com.hungerexpress.restaurant.Restaurant;
import com.hungerexpress.restaurant.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final RestaurantRepository restaurants;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository items;

    public List<MenuCategory> categoriesWithItems(Long restaurantId){
        Restaurant r = restaurants.findById(restaurantId).orElseThrow();
        List<MenuCategory> cats = categories.findByRestaurantOrderByPositionAsc(r);
        // Only show APPROVED items to customers
        cats.forEach(c -> c.setItems(items.findByCategoryIdAndApprovalStatusOrderByIdAsc(c.getId(), MenuItem.ApprovalStatus.APPROVED)));
        return cats;
    }
}
