package com.hotel_service.city_country.repository;

import com.hotel_service.city_country.country_city.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface CityRepository extends JpaRepository<City, UUID> {
    Optional<City>findByCityName(String cityName);
    List<City> findByCityNameIn(List<String> cityNames);
    List<City> findByStates(UUID stateId);
}
