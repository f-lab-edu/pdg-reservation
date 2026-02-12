package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

import java.util.List;

@Getter
public class AccommodationResponse {

    private final long id;
    private final String name;
    private final AccommodationType type;
    private final String city;

    @QueryProjection
    public AccommodationResponse(long id, String name, AccommodationType type, String city) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.city = city;
    }



}
