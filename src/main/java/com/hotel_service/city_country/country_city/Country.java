package com.hotel_service.city_country.country_city;

import com.hotel_service.hotelansrooms.model.BaseModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "country")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country extends BaseModel {
    private String countryName;
    @OneToMany(mappedBy = "country",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<States> stateList;
}
