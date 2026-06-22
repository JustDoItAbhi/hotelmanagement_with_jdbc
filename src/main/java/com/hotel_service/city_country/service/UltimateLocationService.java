package com.hotel_service.city_country.service;

import com.hotel_service.city_country.city_country_dto.*;
import com.hotel_service.city_country.country_city.City;
import com.hotel_service.city_country.country_city.Country;
import com.hotel_service.city_country.country_city.States;
import com.hotel_service.city_country.repository.CityRepository;
import com.hotel_service.city_country.repository.CountryRepository;
import com.hotel_service.city_country.repository.StateRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class UltimateLocationService {

    private final Map<String, UUID> countryCacheMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> cityCacheMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> stateCacheMap = new ConcurrentHashMap<>(); // NEW: State cache
    private final BloomFilter<String> countryBloomFilter;
    private final BloomFilter<String> cityBloomFilter;
    private final BloomFilter<String> stateBloomFilter; // NEW: State Bloom filter
    private final Trie trie = new Trie();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private CityRepository cityRepository;

    public UltimateLocationService(
            BloomFilter<String> countryBloomFilter,
            BloomFilter<String> cityBloomFilter,
            BloomFilter<String> stateBloomFilter) {
        this.countryBloomFilter = countryBloomFilter;
        this.cityBloomFilter = cityBloomFilter;
        this.stateBloomFilter = stateBloomFilter;
    }

    @PostConstruct
    public void initialize() {
        log.info("Starting initialization of caches...");

        // Load countries
        List<Country> countries = countryRepository.findAll();
        for (Country country : countries) {
            String countryName = country.getCountryName();
            countryCacheMap.put(countryName, country.getId());
            countryBloomFilter.put(countryName);
            trie.insert(countryName);
            redisTemplate.opsForHash().put("countries", countryName, country);
        }

        // Load states with their country mapping
        List<States> states = stateRepository.findAll();
        for (States state : states) {
            String stateKey = state.getCountry().getId() + ":" + state.getStateName();
            stateCacheMap.put(stateKey, state.getId());
            stateBloomFilter.put(stateKey);
            redisTemplate.opsForHash().put("states", stateKey, state);
        }

        // Load cities with their state mapping
        List<City> cities = cityRepository.findAll();
        for (City city : cities) {
            String cityKey = city.getStates().getId() + ":" + city.getCityName();
            cityCacheMap.put(cityKey, city.getId());
            cityBloomFilter.put(cityKey);
            redisTemplate.opsForHash().put("cities", cityKey, city);
        }

        log.info("Cache initialization complete. Loaded {} countries, {} states, {} cities",
                countries.size(), states.size(), cities.size());
    }

    public CountryResponseDto createCountryAndCityUltimate(CountryRequestDto dto) {
        long startTime = System.currentTimeMillis();
        String countryName = dto.getCountryName();

        // STEP 1: Get or create country with proper caching
        Country country = getOrCreateCountry(countryName);
        if (country == null) {
            throw new RuntimeException("Failed to get or create country: " + countryName);
        }

        // STEP 2: Process states and cities with caching
        List<States> statesToSave = Collections.synchronizedList(new ArrayList<>());
        List<City> citiesToSave = Collections.synchronizedList(new ArrayList<>());

        List<Future<List<City>>> futures = new ArrayList<>();
        List<StateRequestDto> stateDtos = dto.getStates();

        for (StateRequestDto stateDto : stateDtos) {
            Future<List<City>> future = executor.submit(() ->
                    processStateWithCache(stateDto, country, statesToSave, citiesToSave)
            );
            futures.add(future);
        }

        // STEP 3: Collect results
        for (Future<List<City>> future : futures) {
            try {
                List<City> cities = future.get();
                if (cities != null) {
                    citiesToSave.addAll(cities);
                }
            } catch (Exception e) {
                log.error("Error processing parallel task", e);
            }
        }

        // STEP 4: Batch save
        if (!statesToSave.isEmpty()) {
            saveStatesInBatch(statesToSave);
            // Update caches for new states
            for (States state : statesToSave) {
                updateStateCache(state);
            }
        }

        if (!citiesToSave.isEmpty()) {
            saveCitiesInBatch(citiesToSave);
            // Update caches for new cities
            for (City city : citiesToSave) {
                updateCityCache(city);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Ultimate optimized processing took: {}ms for {} states, {} cities",
                totalTime, stateDtos.size(), citiesToSave.size());

        return buildResponse(country);
    }

    // =============================================
    // GET OR CREATE COUNTRY WITH CACHING
    // =============================================
    private Country getOrCreateCountry(String countryName) {
        // STEP 1: Check in-memory cache
        UUID countryId = countryCacheMap.get(countryName);
        if (countryId != null) {
            // Return from cache without DB query
            Country cachedCountry = (Country) redisTemplate.opsForHash().get("countries", countryName);
            if (cachedCountry != null) {
                log.debug("Country found in memory cache: {}", countryName);
                return cachedCountry;
            }
        }

        // STEP 2: Check Redis
        Country cachedCountry = (Country) redisTemplate.opsForHash().get("countries", countryName);
        if (cachedCountry != null) {
            countryCacheMap.put(countryName, cachedCountry.getId());
            log.debug("Country found in Redis cache: {}", countryName);
            return cachedCountry;
        }

        // STEP 3: Check Bloom filter
        if (countryBloomFilter.mightContain(countryName)) {
            Optional<Country> existingCountry = countryRepository.findByCountryName(countryName);
            if (existingCountry.isPresent()) {
                Country country = existingCountry.get();
                updateCountryCache(country);
                log.debug("Country found in database: {}", countryName);
                return country;
            }
        }

        // STEP 4: Create new country
        Country country = new Country();
        country.setCountryName(countryName);
        country = countryRepository.save(country);
        updateCountryCache(country);
        log.info("Created new country: {}", countryName);
        return country;
    }

    // =============================================
    // PROCESS STATE WITH CACHING
    // =============================================
    private List<City> processStateWithCache(
            StateRequestDto stateDto,
            Country country,
            List<States> statesToSave,
            List<City> citiesToSave) {

        String stateName = stateDto.getStateName();
        String stateKey = country.getId() + ":" + stateName;
        List<City> processedCities = new ArrayList<>();

        // STEP 1: Check if state exists in cache
        States state = getOrCreateState(stateKey, stateName, country);
        if (state == null) {
            log.error("Failed to get or create state: {}", stateName);
            return processedCities;
        }

        // STEP 2: Process cities for this state
        List<CityRequestDto> cityDtos = stateDto.getCities();
        for (CityRequestDto cityDto : cityDtos) {
            String cityName = cityDto.getCityName();
            String cityKey = state.getId() + ":" + cityName;

            // Check if city exists
            if (!cityExists(cityKey)) {
                City city = new City();
                city.setCityName(cityName);
                city.setCityImage(cityDto.getCityImage());
                city.setStates(state);
                citiesToSave.add(city);
                processedCities.add(city);
                log.debug("Preparing to save new city: {} in state: {}", cityName, stateName);
            } else {
                log.debug("City already exists: {}", cityName);
            }
        }

        return processedCities;
    }

    // =============================================
    // GET OR CREATE STATE WITH CACHING
    // =============================================
    private States getOrCreateState(String stateKey, String stateName, Country country) {
        // Check memory cache
        UUID stateId = stateCacheMap.get(stateKey);
        if (stateId != null) {
            States cachedState = (States) redisTemplate.opsForHash().get("states", stateKey);
            if (cachedState != null) {
                return cachedState;
            }
        }

        // Check Redis
        States cachedState = (States) redisTemplate.opsForHash().get("states", stateKey);
        if (cachedState != null) {
            stateCacheMap.put(stateKey, cachedState.getId());
            return cachedState;
        }

        // Check Bloom filter
        if (stateBloomFilter.mightContain(stateKey)) {
            Optional<States> existingState = stateRepository.findByCountryIdAndStateName(
                    country.getId(), stateName);
            if (existingState.isPresent()) {
                States state = existingState.get();
                updateStateCache(state);
                return state;
            }
        }

        // Create new state
        States state = new States();
        state.setStateName(stateName);
        state.setCountry(country);
        return state; // Will be saved in batch
    }

    // =============================================
    // CHECK IF CITY EXISTS USING CACHE
    // =============================================
    private boolean cityExists(String cityKey) {
        // Check memory cache
        if (cityCacheMap.containsKey(cityKey)) {
            return true;
        }

        // Check Redis
        if (redisTemplate.opsForHash().hasKey("cities", cityKey)) {
            cityCacheMap.put(cityKey, (UUID) redisTemplate.opsForHash().get("cities", cityKey));
            return true;
        }

        // Check Bloom filter
        if (cityBloomFilter.mightContain(cityKey)) {
            // Could exist, but we'll verify during batch save with ON CONFLICT
            return true;
        }

        return false;
    }

    // =============================================
    // CACHE UPDATE METHODS
    // =============================================
    private void updateCountryCache(Country country) {
        String countryName = country.getCountryName();
        countryCacheMap.put(countryName, country.getId());
        countryBloomFilter.put(countryName);
        trie.insert(countryName);
        redisTemplate.opsForHash().put("countries", countryName, country);
    }

    private void updateStateCache(States state) {
        String stateKey = state.getCountry().getId() + ":" + state.getStateName();
        stateCacheMap.put(stateKey, state.getId());
        stateBloomFilter.put(stateKey);
        redisTemplate.opsForHash().put("states", stateKey, state);
    }

    private void updateCityCache(City city) {
        String cityKey = city.getStates().getId() + ":" + city.getCityName();
        cityCacheMap.put(cityKey, city.getId());
        cityBloomFilter.put(cityKey);
        redisTemplate.opsForHash().put("cities", cityKey, city);
    }

    // =============================================
    // BATCH SAVE METHODS (Optimized)
    // =============================================
    private void saveStatesInBatch(List<States> states) {
        if (states.isEmpty()) return;

        List<Object[]> batchArgs = new ArrayList<>();
        for (States state : states) {
            // Generate UUID if not set
            if (state.getId() == null) {
                state.setId(UUID.randomUUID());
            }

            Object[] values = new Object[5];
            values[0] = state.getId();
            values[1] = state.getStateName();
            values[2] = state.getCountry().getId();
            values[3] = Instant.now();
            values[4] = Instant.now();
            batchArgs.add(values);
        }

        String sql = "INSERT INTO states (id, state_name, country_id, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (state_name, country_id) DO UPDATE SET updated_at = EXCLUDED.updated_at " +
                "RETURNING id";

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void saveCitiesInBatch(List<City> cities) {
        if (cities.isEmpty()) return;

        List<Object[]> batchArgs = new ArrayList<>();
        for (City city : cities) {
            if (city.getId() == null) {
                city.setId(UUID.randomUUID());
            }

            Object[] values = new Object[6];
            values[0] = city.getId();
            values[1] = city.getCityName();
            values[2] = city.getStates().getId();
            values[3] = city.getCityImage();
            values[4] = Instant.now();
            values[5] = Instant.now();
            batchArgs.add(values);
        }

        String sql = "INSERT INTO city (id, city_name, state_id, city_image, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (city_name, state_id) DO UPDATE SET updated_at = EXCLUDED.updated_at";

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    // =============================================
    // BUILD RESPONSE (Optimized with caching)
    // =============================================
    private CountryResponseDto buildResponse(Country country) {
        CountryResponseDto response = new CountryResponseDto();
        response.setCountryName(country.getCountryName());

        // Get states with caching
        List<States> states = getStatesWithCache(country.getId());
        List<StateResponseDto> stateResponseList = new ArrayList<>();

        for (States state : states) {
            StateResponseDto stateResponse = new StateResponseDto();
            stateResponse.setStateName(state.getStateName());

            // Get cities with caching
            List<City> cities = getCitiesWithCache(state.getId());
            List<CityResponseDto> cityResponseList = new ArrayList<>();

            for (City city : cities) {
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

    private List<States> getStatesWithCache(UUID countryId) {
        // Try Redis first
        String cacheKey = "country_states:" + countryId;
        List<States> cachedStates = (List<States>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStates != null) {
            return cachedStates;
        }

        // Fetch from database
        List<States> states = stateRepository.findByCountryId(countryId);
        redisTemplate.opsForValue().set(cacheKey, states, 1, TimeUnit.HOURS);
        return states;
    }

    private List<City> getCitiesWithCache(UUID stateId) {
        String cacheKey = "state_cities:" + stateId;
        List<City> cachedCities = (List<City>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedCities != null) {
            return cachedCities;
        }

        List<City> cities = cityRepository.findByStates(stateId);
        redisTemplate.opsForValue().set(cacheKey, cities, 1, TimeUnit.HOURS);
        return cities;
    }
}