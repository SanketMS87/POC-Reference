package com.rukamini.eat.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Comma-separated cuisine names, e.g. "Biryani,North Indian". */
    @Column(nullable = false)
    private String cuisines;

    private double rating;
    private int etaMinutes;
    private int costForTwo;
    private boolean pureVeg;
    private String emoji;

    @Column(length = 300)
    private String description;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<MenuItem> menu = new ArrayList<>();

    protected Restaurant() {
    }

    public Restaurant(String name, String cuisines, double rating, int etaMinutes, int costForTwo,
                      boolean pureVeg, String emoji, String description) {
        this.name = name;
        this.cuisines = cuisines;
        this.rating = rating;
        this.etaMinutes = etaMinutes;
        this.costForTwo = costForTwo;
        this.pureVeg = pureVeg;
        this.emoji = emoji;
        this.description = description;
    }

    public void addMenuItem(MenuItem item) {
        item.setRestaurant(this);
        menu.add(item);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCuisines() { return cuisines; }
    public double getRating() { return rating; }
    public int getEtaMinutes() { return etaMinutes; }
    public int getCostForTwo() { return costForTwo; }
    public boolean isPureVeg() { return pureVeg; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public List<MenuItem> getMenu() { return menu; }
}
