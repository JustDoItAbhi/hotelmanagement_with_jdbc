package com.hotel_service.hotelansrooms.model.hotel_rooms;

import jakarta.persistence.Embeddable;

import java.util.UUID;

@Embeddable
public class Address {
    private String countryName;
    private String  stateName;
    private String  cityId;
    private String street;
    private String buildingNumber;
    private String zipCode;
    private String nearByLocation;
}
