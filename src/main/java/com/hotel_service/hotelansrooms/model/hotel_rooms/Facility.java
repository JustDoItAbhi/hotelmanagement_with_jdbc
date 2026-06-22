package com.hotel_service.hotelansrooms.model.hotel_rooms;

import com.hotel_service.hotelansrooms.model.BaseModel;
import com.hotel_service.hotelansrooms.model.FacilityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name="facility")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Facility extends BaseModel {
    @Enumerated(EnumType.STRING)
    private FacilityType facilityType;
    @ManyToMany(mappedBy = "facilitiesList")
    private List<Room> rooms;
}
