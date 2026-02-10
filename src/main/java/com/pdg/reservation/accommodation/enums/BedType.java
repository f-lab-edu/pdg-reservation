package com.pdg.reservation.accommodation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BedType {
    SINGLE("싱글"),
    DOUBLE("더블"),
    TWIN("트윈"),
    KING("킹")
    ;

    private final String description;
}

