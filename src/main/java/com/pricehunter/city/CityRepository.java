package com.pricehunter.city;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CityRepository extends JpaRepository<City, UUID> {

    Optional<City> findByCountryCodeAndNormalizedName(String countryCode, String normalizedName);
}
