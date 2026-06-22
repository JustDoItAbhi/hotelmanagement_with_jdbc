package com.hotel_service.city_country.service;

import com.hotel_service.city_country.city_country_dto.*;
import com.hotel_service.city_country.country_city.City;
import com.hotel_service.city_country.country_city.Country;
import com.hotel_service.city_country.country_city.States;
import com.hotel_service.city_country.mapper.StateMapper;
import com.hotel_service.city_country.repository.CityRepository;
import com.hotel_service.city_country.repository.CountryRepository;
import com.hotel_service.city_country.repository.StateRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CountryServiceImpl implements CountryService{
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final StateRepository stateRepository;
    private final ModelMapper mapper;

    public CountryServiceImpl(CountryRepository countryRepository, CityRepository cityRepository, StateRepository stateRepository, ModelMapper mapper) {
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.stateRepository = stateRepository;
        this.mapper = mapper;
    }

    @Override
    public CountryResponseDto createCountryAndCity(CountryRequestDto dto) {
        Optional<Country> exsistingCountry=countryRepository.findByCountryName(dto.getCountryName());
        if(exsistingCountry.isPresent()){
            return mapper.map(exsistingCountry.get(),CountryResponseDto.class);
        }
        Country country=new Country();
        country.setCountryName(dto.getCountryName());
        countryRepository.save(country);

        List<States>states=new ArrayList<>();

       for(StateRequestDto stateRequestDto:dto.getStates()){
//           List<States> oldAllState=stateRepository.findByStateNameIn(stateRequestDto);
           Optional<States>exsistingStates=stateRepository.findByStateName(stateRequestDto.getStateName());
           if(exsistingStates.isPresent()){
               throw new RuntimeException("STATE ALREADY EXSITS :: "+ stateRequestDto.getStateName());
           }
           States newStates=new States();
           newStates.setStateName(stateRequestDto.getStateName());
           newStates.setCountry(country);
           stateRepository.save(newStates);


           for(CityRequestDto cityRequestDto:stateRequestDto.getCities()){
               Optional<City>cityOptional=cityRepository.findByCityName(cityRequestDto.getCityName());
               if(cityOptional.isPresent()){
                   throw new RuntimeException("CITY ALREADY EXSITS :: "+ cityRequestDto.getCityName());
               }
               City city=new City();
               city.setCityName(cityRequestDto.getCityName());
               city.setCityImage(cityRequestDto.getCityImage());

               cityRepository.save(city);
               city.setStates(newStates);
           }

           states.add(newStates);
       }
        country.setStateList(states);
       countryRepository.save(country);

        return mapper.map(country, CountryResponseDto.class);
    }


    @Override
    public boolean deleteCountry(UUID countryId) {
        return false;
    }

    @Override
    public CountryResponseDto updateCountryName(String countryName) {
        return null;
    }

    @Override
    public Page<CountryResponseDto> getAllCountriesAndCities(int page, int pageSize) {
        return null;
    }
}
