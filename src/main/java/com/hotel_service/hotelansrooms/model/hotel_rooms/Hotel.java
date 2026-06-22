package com.hotel_service.hotelansrooms.model.hotel_rooms;

import com.hotel_service.city_country.country_city.City;
import com.hotel_service.hotelansrooms.model.BaseModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "hotel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hotel extends BaseModel {
    private String hotelName;
    private String description;

    @ElementCollection
    @CollectionTable(name = "hotel_addresses", joinColumns = @JoinColumn(name = "hotel_id"))
    private List<Address> addressList;
    @Embedded
    private Rating rating;

    private String checkinAndCheckoutTime;
    @OneToMany(mappedBy = "hotel",fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Room>roomList;
    private boolean breakFast;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;
}
