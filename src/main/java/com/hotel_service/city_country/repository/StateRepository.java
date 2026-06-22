package com.hotel_service.city_country.repository;

import com.hotel_service.city_country.country_city.States;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface StateRepository extends JpaRepository<States, UUID> {
Optional<States>findByStateName(String name);
    List<States> findByStateNameIn(List<String> names);
    List<States>findByCountryId(UUID countryId);
    List<States> findByStateNameInAndCountryId(List<String> stateNames, UUID countryId);

    Optional<States> findByStateNameAndCountryId(String stateName, UUID countryId);
    Optional<States>findByCountryIdAndStateName(UUID ID, String name);

}
