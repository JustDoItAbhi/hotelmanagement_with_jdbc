package com.hotel_service.hotelansrooms.model.hotel_rooms;

import com.hotel_service.hotelansrooms.model.BaseModel;
import com.hotel_service.hotelansrooms.model.RoomType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Room extends BaseModel {
    @Enumerated(EnumType.STRING)
    private RoomType roomType;
    @Embedded
    private Price price;
    @ManyToMany
    @JoinTable(
            name = "room_facility",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "facility_id")
    )
    private List<Facility> facilitiesList;
    private int numberOfPeopleAllowed;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;
}
