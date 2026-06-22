package com.hotel_service.city_country.city_country_dto;

import lombok.Data;

import java.util.List;

@Data
public class StateRequestDto {
    private String stateName;
    private List<CityRequestDto> cities;
}
