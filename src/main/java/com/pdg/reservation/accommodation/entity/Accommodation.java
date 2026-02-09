package com.pdg.reservation.accommodation.entity;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.pdg.reservation.accommodation.enums.Type;
import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'HOTEL'")
    private Type type = Type.HOTEL;

    @Column
    private String description;

    @Column
    private String amenities;

    @Column(nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    @Column(precision = 3, scale = 1, nullable = false)
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
