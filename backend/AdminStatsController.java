package com.hungerexpress.admin;

import com.hungerexpress.orders.OrderEntity;
import com.hungerexpress.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {
    private final OrderRepository orders;

    record Series(List<String> labels, List<Number> values){}
    record Top(String name, Number value){}

    @GetMapping("/orders-per-day")
    public ResponseEntity<Series> ordersPerDay(){
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i=6;i>=0;i--){
            String d = today.minusDays(i).toString(); labels.add(d); map.put(d, 0);
        }
        orders.findAll().forEach(o -> {
            String d = LocalDate.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()).toString();
            if (map.containsKey(d)) map.put(d, map.get(d)+1);
        });
        return ResponseEntity.ok(new Series(labels, new ArrayList<>(map.values())));
    }

    @GetMapping("/gmv-per-day")
    public ResponseEntity<Series> gmvPerDay(){
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (int i=6;i>=0;i--){
            String d = today.minusDays(i).toString(); labels.add(d); map.put(d, BigDecimal.ZERO);
        }
        orders.findAll().forEach(o -> {
            String d = LocalDate.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()).toString();
            if (map.containsKey(d)) map.put(d, map.get(d).add(o.getTotal()));
        });
        List<Number> values = map.values().stream()
                .map(BigDecimal::doubleValue)
                .map(d -> (Number) d)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new Series(labels, values));
    }

    @GetMapping("/top-restaurants")
    public ResponseEntity<List<Top>> topRestaurants(){
        Map<Long, BigDecimal> sums = new HashMap<>();
        orders.findAll().forEach(o -> {
            Long rid = o.getRestaurantId()!=null? o.getRestaurantId() : 0L;
            sums.put(rid, sums.getOrDefault(rid, BigDecimal.ZERO).add(o.getTotal()));
        });
        List<Top> tops = sums.entrySet().stream()
                .sorted((a,b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(e -> new Top("R-"+e.getKey(), e.getValue()))
                .toList();
        return ResponseEntity.ok(tops);
    }
}
