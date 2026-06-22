package com.hotel_service.city_country.mapper;

import com.hotel_service.city_country.city_country_dto.CityResponseDto;
import com.hotel_service.city_country.city_country_dto.StateResponseDto;
import com.hotel_service.city_country.country_city.City;
import com.hotel_service.city_country.country_city.States;

import java.util.ArrayList;
import java.util.List;

public class StateMapper {
    public static StateResponseDto fromState(States state){
        StateResponseDto dto=new StateResponseDto();
        dto.setStateId(state.getId());
        dto.setStateName(state.getStateName());
        List<CityResponseDto>cityResponseDtos=new ArrayList<>();
        for(City city   :state.getCityList()) {
            CityResponseDto responseDto = new CityResponseDto();
            responseDto.setCityid(city.getId());
            responseDto.setCityName(city.getCityName());
            responseDto.setCityImage(city.getCityImage());
            cityResponseDtos.add(responseDto);
        }
        dto.setCityResponseDtoList(cityResponseDtos);
        return dto;
    }
}
