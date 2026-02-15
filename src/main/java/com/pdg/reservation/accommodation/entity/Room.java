package com.pdg.reservation.accommodation.entity;

import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;

@Entity
@Table(name = "room", indexes = {
        @Index(name = "idx_accommodation_id", columnList = "accommodation_id")
})
@Getter
@Builder
@DynamicInsert
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SINGLE'")
    @Builder.Default
    private BedType bedType = BedType.SINGLE;

    @Column
    private String description;

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean isReservable = false;

    @Column(length = 50)
    private String roomLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_Id", nullable = false)
    private Accommodation accommodation;




}
