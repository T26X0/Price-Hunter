package com.pricehunter.city;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Доступ к справочнику городов и поиск уже существующего города без дублей. */
public interface CityRepository extends JpaRepository<City, UUID> {

    /** Находит город по стране и нормализованному названию. */
    Optional<City> findByCountryCodeAndNormalizedName(String countryCode, String normalizedName);
}
