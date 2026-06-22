package com.hotel_service.hotelansrooms.model.hotel_rooms;

import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class Price {
private BigDecimal price;
private BigDecimal taxes;
private BigDecimal discount;
}
