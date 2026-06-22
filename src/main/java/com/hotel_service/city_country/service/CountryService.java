package com.hotel_service.city_country.service;

import com.hotel_service.city_country.city_country_dto.CountryRequestDto;
import com.hotel_service.city_country.city_country_dto.CountryResponseDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CountryService {
    CountryResponseDto createCountryAndCity(CountryRequestDto dto);
    boolean deleteCountry(UUID countryId);
    CountryResponseDto updateCountryName(String countryName);
    Page<CountryResponseDto>getAllCountriesAndCities(int page, int pageSize);


}
