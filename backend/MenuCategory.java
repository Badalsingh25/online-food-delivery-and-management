package com.hungerexpress.menu;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hungerexpress.restaurant.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_category")
@Getter 
@Setter
 @NoArgsConstructor 
 @AllArgsConstructor
 @Builder
public class MenuCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "restaurant_id")
    @JsonIgnore  // Prevent circular reference
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer position = 0;

    @OneToMany(mappedBy = "category")
    @Builder.Default
    @Transient  // Don't load from DB automatically, manually set in service
    private List<MenuItem> items = new ArrayList<>();
}
