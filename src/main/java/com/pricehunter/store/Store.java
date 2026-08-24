package com.pricehunter.store;

import com.pricehunter.city.City;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany
    @JoinTable(
            name = "store_cities",
            joinColumns = @JoinColumn(name = "store_id"),
            inverseJoinColumns = @JoinColumn(name = "city_id")
    )
    private Set<City> cities = new HashSet<>();
}
