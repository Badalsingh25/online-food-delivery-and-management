package com.hungerexpress.cart;

import com.hungerexpress.common.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/cart/items")
@RequiredArgsConstructor
public class CartController_OLD {

    private final Map<String, List<CartItemDto>> carts = new ConcurrentHashMap<>();

    private List<CartItemDto> cart(){
        String email = Optional.ofNullable(CurrentUser.email()).orElse("guest");
        return carts.computeIfAbsent(email, k -> new ArrayList<>());
    }

    @GetMapping
    public List<CartItemDto> list(){
        return new ArrayList<>(cart());
    }

    @PostMapping
    public List<CartItemDto> add(@RequestBody CartItemDto item){
        List<CartItemDto> c = cart();
        for (int i=0;i<c.size();i++){
            if (Objects.equals(c.get(i).id(), item.id())){
                CartItemDto cur = c.get(i);
                c.set(i, new CartItemDto(cur.id(), cur.name(), cur.price(), cur.imageUrl(), cur.qty()+Math.max(1,item.qty())));
                return c;
            }
        }
        c.add(new CartItemDto(item.id(), item.name(), item.price(), item.imageUrl(), Math.max(1, item.qty())));
        return c;
    }

    @PatchMapping("/{id}")
    public List<CartItemDto> change(@PathVariable Long id, @RequestParam int delta){
        List<CartItemDto> c = cart();
        for (int i=0;i<c.size();i++){
            if (Objects.equals(c.get(i).id(), id)){
                CartItemDto cur = c.get(i);
                int q = Math.max(1, cur.qty()+delta);
                c.set(i, new CartItemDto(cur.id(), cur.name(), cur.price(), cur.imageUrl(), q));
                break;
            }
        }
        return c;
    }

    @DeleteMapping("/{id}")
    public List<CartItemDto> remove(@PathVariable Long id){
        List<CartItemDto> c = cart();
        c.removeIf(it -> Objects.equals(it.id(), id));
        return c;
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(){
        cart().clear();
        return ResponseEntity.noContent().build();
    }
}
