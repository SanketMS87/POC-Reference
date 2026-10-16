package com.rukamini.eat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;

@Entity
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private boolean veg;
    private boolean bestseller;
    private String emoji;

    protected MenuItem() {
    }

    public MenuItem(String name, String category, BigDecimal price, boolean veg, boolean bestseller, String emoji) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.veg = veg;
        this.bestseller = bestseller;
        this.emoji = emoji;
    }

    void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public Long getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public boolean isVeg() { return veg; }
    public boolean isBestseller() { return bestseller; }
    public String getEmoji() { return emoji; }
}
