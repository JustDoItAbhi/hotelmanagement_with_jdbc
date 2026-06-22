package com.hotel_service.city_country.service;

import com.hotel_service.city_country.city_country_dto.*;
import com.hotel_service.city_country.country_city.City;
import com.hotel_service.city_country.country_city.Country;
import com.hotel_service.city_country.country_city.States;
import com.hotel_service.city_country.repository.CityRepository;
import com.hotel_service.city_country.repository.CountryRepository;
import com.hotel_service.city_country.repository.StateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional
public class saveAllLocationService {
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private CityRepository cityRepository;

    public CountryResponseDto createCountryAndCity(CountryRequestDto dto) {

        long startTime = System.currentTimeMillis();

        try {
            // ============ STEP 1: Get or Create Country ============
            Country country = getOrCreateCountry(dto.getCountryName());

            // ============ STEP 2: Extract All State Names ============
            List<StateRequestDto> stateDtos = dto.getStates();
            List<String> stateNames = new ArrayList<>();

            for (int i = 0; i < stateDtos.size(); i++) {
                String stateName = stateDtos.get(i).getStateName();
                stateNames.add(stateName);
            }

            // ============ STEP 3: Extract All City Names ============
            List<String> cityNames = new ArrayList<>();

            for (int i = 0; i < stateDtos.size(); i++) {
                StateRequestDto stateDto = stateDtos.get(i);
                List<CityRequestDto> cityDtos = stateDto.getCities();

                for (int j = 0; j < cityDtos.size(); j++) {
                    String cityName = cityDtos.get(j).getCityName();
                    cityNames.add(cityName);
                }
            }

            // ============ STEP 4: Bulk Fetch Existing States ============
            // This is ONE database call to get ALL existing states
            List<States> existingStateList = stateRepository
                    .findByStateNameInAndCountryId(stateNames, country.getId());

            // Put existing state names in a Set for quick lookup
            Set<String> existingStateSet = new HashSet<>();
            for (int i = 0; i < existingStateList.size(); i++) {
                States state = existingStateList.get(i);
                existingStateSet.add(state.getStateName());
            }

            // ============ STEP 5: Bulk Fetch Existing Cities ============
            // This is ONE database call to get ALL existing cities
            List<City> existingCityList = cityRepository.findByCityNameIn(cityNames);

            // Put existing city names in a Set for quick lookup
            Set<String> existingCitySet = new HashSet<>();
            for (int i = 0; i < existingCityList.size(); i++) {
                City city = existingCityList.get(i);
                existingCitySet.add(city.getCityName());
            }

            // ============ STEP 6: Prepare States to Save ============
            List<States> statesToSave = new ArrayList<>();
            Map<String, States> stateMap = new HashMap<>();

            for (int i = 0; i < stateDtos.size(); i++) {
                StateRequestDto stateDto = stateDtos.get(i);
                String stateName = stateDto.getStateName();

                // Only create state if it doesn't already exist
                if (!existingStateSet.contains(stateName)) {
                    States newState = new States();
                    newState.setStateName(stateName);
                    newState.setCountry(country);

                    statesToSave.add(newState);
                    stateMap.put(stateName, newState);
                }
            }

            // ============ STEP 7: Batch Save States ============
            // This is ONE database call to save ALL new states
            if (!statesToSave.isEmpty()) {
                List<States> savedStates = stateRepository.saveAll(statesToSave);

                // Update the map with saved states (now they have IDs)
                for (int i = 0; i < savedStates.size(); i++) {
                    States savedState = savedStates.get(i);
                    stateMap.put(savedState.getStateName(), savedState);
                }
            }

            // ============ STEP 8: Prepare Cities to Save ============
            List<City> citiesToSave = new ArrayList<>();

            for (int i = 0; i < stateDtos.size(); i++) {
                StateRequestDto stateDto = stateDtos.get(i);
                String stateName = stateDto.getStateName();

                // Get the state object (either new or existing)
                States state = stateMap.get(stateName);

                if (state == null) {
                    // State already existed, fetch it from database
                    state = stateRepository
                            .findByStateNameAndCountryId(stateName, country.getId())
                            .orElseThrow(() -> new RuntimeException("State not found"));
                    stateMap.put(stateName, state);
                }

                // Now process cities for this state
                List<CityRequestDto> cityDtos = stateDto.getCities();

                for (int j = 0; j < cityDtos.size(); j++) {
                    CityRequestDto cityDto = cityDtos.get(j);
                    String cityName = cityDto.getCityName();

                    // Only create city if it doesn't already exist
                    if (!existingCitySet.contains(cityName)) {
                        City newCity = new City();
                        newCity.setCityName(cityName);
                        newCity.setCityImage(cityDto.getCityImage());
                        newCity.setStates(state); // Link to state

                        citiesToSave.add(newCity);
                    }
                }
            }

            // ============ STEP 9: Batch Save Cities ============
            // This is ONE database call to save ALL new cities
            if (!citiesToSave.isEmpty()) {
                cityRepository.saveAll(citiesToSave);
            }

            // ============ STEP 10: Build Response ============
            long endTime = System.currentTimeMillis();
            log.info("Created location data in {}ms. States: {}, Cities: {}",
                    endTime - startTime, statesToSave.size(), citiesToSave.size());

            return buildCompleteResponse(country);

        } catch (Exception e) {
            log.error("Error creating location data", e);
            throw new RuntimeException("Failed to create location data", e);
        }
    }

    // ============ HELPER METHODS ============

    private Country getOrCreateCountry(String countryName) {
        Optional<Country> existingCountry = countryRepository.findByCountryName(countryName);

        if (existingCountry != null) {
            return existingCountry.get();
        }

        Country newCountry = new Country();
        newCountry.setCountryName(countryName);
        return countryRepository.save(newCountry);
    }

    private CountryResponseDto buildCompleteResponse(Country country) {
        // Fetch complete data with all relationships in ONE query
        Country completeCountry = countryRepository
                .findByIdWithStatesAndCities(country.getId())
                .orElseThrow(() -> new RuntimeException("Country not found"));

        // Build response DTO
        CountryResponseDto response = new CountryResponseDto();
        response.setCountryName(completeCountry.getCountryName());

        List<StateResponseDto> stateResponseList = new ArrayList<>();
        List<States> states = completeCountry.getStateList();

        for (int i = 0; i < states.size(); i++) {
            States state = states.get(i);

            StateResponseDto stateResponse = new StateResponseDto();
            stateResponse.setStateName(state.getStateName());

            List<CityResponseDto> cityResponseList = new ArrayList<>();
            List<City> cities = state.getCityList();

            for (int j = 0; j < cities.size(); j++) {
                City city = cities.get(j);

                CityResponseDto cityResponse = new CityResponseDto();
                cityResponse.setCityName(city.getCityName());
                cityResponse.setCityImage(city.getCityImage());

                cityResponseList.add(cityResponse);
            }

            stateResponse.setCityResponseDtoList(cityResponseList);
            stateResponseList.add(stateResponse);
        }

        response.setStateResponseDtosList(stateResponseList);
        return response;
    }
}
/*
saveAll Approach (Strategy 1)
// O(n) for states + O(n*m) for cities
List<String> stateNames = extractStateNames(dto);     // O(n)
List<String> cityNames = extractCityNames(dto);       // O(n*m)

Set<String> existingStates = stateRepository
    .findByStateNameIn(stateNames);                   // O(1) DB call

Set<String> existingCities = cityRepository
    .findByCityNameIn(cityNames);                     // O(1) DB call

List<States> statesToSave = prepareStates();          // O(n)
List<City> citiesToSave = prepareCities();            // O(n*m)

stateRepository.saveAll(statesToSave);               // O(1) DB call
cityRepository.saveAll(citiesToSave);                // O(1) DB call
 */

/*
   {
            "countryName": "India",
            "states": [
                {
                    "stateName": "Maharashtra",
                    "cities": [
                        {"cityName": "Mumbai", "cityImage": "mumbai.jpg"},
                        {"cityName": "Pune", "cityImage": "pune.jpg"}
                    ]
                },
                {
                    "stateName": "Karnataka",
                    "cities": [
                        {"cityName": "Bangalore", "cityImage": "bangalore.jpg"},
                        {"cityName": "Mysore", "cityImage": "mysore.jpg"}
                    ]
                }
            ]
        }
        */
