package com.pdg.reservation.accommodation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccommodationType {
    HOTEL("호텔"),
    MOTEL("모텔"),
    GUEST_HOUSE("게스트 하우스")
    ;

    private final String description;

}
