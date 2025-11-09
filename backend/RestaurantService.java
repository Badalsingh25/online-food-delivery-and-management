package com.hungerexpress.restaurant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository repository;

    public List<Restaurant> list(String search, Cuisine cuisine, Double minRating) {
        return repository.search(
            (search == null || search.isBlank()) ? null : search,
            cuisine,
            minRating
        );
    }
}
