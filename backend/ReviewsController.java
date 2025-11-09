package com.hungerexpress.reviews;

import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewsController {
    private final ReviewRepository reviews;
    private final UserRepository users;

    private Long currentUserId(){
        String email = Optional.ofNullable(CurrentUser.email()).orElse(null);
        if (email == null) return null;
        return users.findByEmail(email).map(User::getId).orElse(null);
    }

    @GetMapping("/menu/{menuItemId}")
    public List<ReviewEntity> listByItem(@PathVariable Long menuItemId){
        return reviews.findByMenuItemIdOrderByCreatedAtDesc(menuItemId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ReviewEntity> listByRestaurant(@PathVariable Long restaurantId){
        return reviews.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    record CreateReview(Integer rating, String comment){}

    @PostMapping("/menu/{menuItemId}")
    public ResponseEntity<ReviewEntity> createForItem(@PathVariable Long menuItemId, @RequestBody CreateReview req){
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.status(401).build();
        if (req.rating == null || req.rating < 1 || req.rating > 5) return ResponseEntity.badRequest().build();
        ReviewEntity r = ReviewEntity.builder()
                .userId(uid)
                .menuItemId(menuItemId)
                .rating(req.rating)
                .comment(req.comment)
                .build();
        return ResponseEntity.ok(reviews.save(r));
    }
}
