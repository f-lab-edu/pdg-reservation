package com.pdg.reservation.accommodation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class Address {

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false)
    private String zipcode;

    @Column(nullable = false)
    private String street;

}
