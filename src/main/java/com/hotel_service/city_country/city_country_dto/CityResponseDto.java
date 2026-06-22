package com.hotel_service.city_country.city_country_dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CityResponseDto {
    private UUID cityid;
    private String cityName;
    private String cityImage;
}
