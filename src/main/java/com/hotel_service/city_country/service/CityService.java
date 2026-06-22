package com.hotel_service.city_country.service;

import com.hotel_service.city_country.city_country_dto.CityResponseDto;

import java.util.List;
import java.util.UUID;

public interface CityService {
    List<CityResponseDto> getAllCitiesByCountryId(UUID countryId);
}
