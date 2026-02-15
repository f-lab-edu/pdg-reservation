package com.pdg.reservation.accommodation.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {
    ACCOMMODATION("숙소"),
    ROOM("객실")
    ;

    private final String description;
}
