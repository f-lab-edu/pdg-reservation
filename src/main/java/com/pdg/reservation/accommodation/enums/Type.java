package com.pdg.reservation.accommodation.enums;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
public enum Type {
    HOTEL("호텔"),
    MOTEL("모텔"),
    GUEST_HOUSE("게스트 하우스")
    ;

    final String description;

}
