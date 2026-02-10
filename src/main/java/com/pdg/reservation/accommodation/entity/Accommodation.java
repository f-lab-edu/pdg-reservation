package com.pdg.reservation.accommodation.entity;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accommodation")
@Getter
@Builder
@DynamicInsert // null인 필드는 insert 쿼리에서 제외시킴 -> DB Default값 발동
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Accommodation extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'HOTEL'")
    @Builder.Default
    private AccommodationType type = AccommodationType.HOTEL;

    @Column
    private String description;

    @Column
    private String amenities;

    @Column(nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    @Column(nullable = false, precision = 3, scale = 1)
    @ColumnDefault("0.0")
    @Builder.Default
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean isOperational = false;

    @Embedded
    private Address address;




}
