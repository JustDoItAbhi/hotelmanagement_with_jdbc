package com.hotel_service.city_country.country_city;

import com.hotel_service.hotelansrooms.model.BaseModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "states")
@Getter
@Setter
public class States extends BaseModel {
private String stateName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;
    @OneToMany(mappedBy = "states",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<City> cityList;
}
