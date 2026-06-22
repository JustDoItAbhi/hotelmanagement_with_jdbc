package com.hotel_service.city_country.city_country_dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CountryResponseDto {
    private UUID countryId;
    private String countryName;
    private List<StateResponseDto> stateResponseDtosList;
}
