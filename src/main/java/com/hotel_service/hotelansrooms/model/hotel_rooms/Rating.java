package com.hotel_service.hotelansrooms.model.hotel_rooms;

import jakarta.persistence.Embeddable;

@Embeddable
public class Rating {
    private int star;
    private String review;

}
