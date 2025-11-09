package com.hungerexpress.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{id}/menu")
@RequiredArgsConstructor
public class MenuController {
    private final MenuService menu;

    @GetMapping
    public List<MenuCategory> get(@PathVariable("id") Long restaurantId){
        return menu.categoriesWithItems(restaurantId);
    }
}
