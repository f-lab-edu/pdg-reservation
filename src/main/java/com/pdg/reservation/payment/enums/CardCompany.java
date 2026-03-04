package com.pdg.reservation.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CardCompany {

    SHINHAN("신한카드"),
    KB("KB국민카드"),
    SAMSUNG("삼성카드"),
    WOORI("우리카드"),
    ;

    private final String description;
}