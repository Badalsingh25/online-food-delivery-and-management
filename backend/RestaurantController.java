package com.hungerexpress.restaurant;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService service;

    @GetMapping
    public List<Restaurant> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Cuisine cuisine,
            @RequestParam(required = false) Double minRating
    ) {
        return service.list(search, cuisine, minRating);
    }
}
