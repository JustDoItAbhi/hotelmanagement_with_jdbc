package com.hotel_service.city_country.mapper;

import com.hotel_service.city_country.city_country_dto.CityResponseDto;
import com.hotel_service.city_country.city_country_dto.CountryResponseDto;
import com.hotel_service.city_country.country_city.City;
import com.hotel_service.city_country.country_city.Country;
import org.modelmapper.ModelMapper;

public class CountryCityMapper {
private final ModelMapper mapper;

    public CountryCityMapper(ModelMapper mapper) {
        this.mapper = mapper;
    }
    public CountryResponseDto fromCountryEntity(Country country){
        CountryResponseDto dto=mapper.map(country,CountryResponseDto.class);
        return dto;
    }
    public CityResponseDto fromCityEntity(City city){
        CityResponseDto dto=mapper.map(city,CityResponseDto.class);
        return dto;
    }
}
