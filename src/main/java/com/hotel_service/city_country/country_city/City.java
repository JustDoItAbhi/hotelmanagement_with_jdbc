package com.hotel_service.city_country.country_city;

import com.hotel_service.hotelansrooms.model.BaseModel;
import com.hotel_service.hotelansrooms.model.hotel_rooms.Hotel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "city")
@Getter
@Setter
public class City extends BaseModel {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "states_id")
    private States states;
    private String cityName;
    private String cityImage;
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Hotel> hotelList;
}
