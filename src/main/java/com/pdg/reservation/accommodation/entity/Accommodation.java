package com.pdg.reservation.accommodation.entity;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.member.entity.QMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "accommodation", indexes = {
        @Index(name = "idx_city_type", columnList = "city, type")
})
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

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

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private List<AccommodationImage> images = new ArrayList<>();

    /*연관 관계 생성 편의 메서드*/
    public void addImage(AccommodationImage image) {
        //1.중복 방지 추가
        if(images.contains(image)){
            return;
        }
        //2. 현재 이미지 컬렉션에 추가
        images.add(image);
        //3. 연관 관계 주인 통해 FK 변경
        image.changeAccommodation(this);
    }

    /*연관 관계 제거 편의 메서드*/
    public void removeImage(AccommodationImage image) {
        // 1. 현재 숙소에 속한 이미지가 아니면 아무것도 안함
        if(!images.contains(image)){
            return;
        }
        // 2. 컬렉션에서 제거
        images.remove(image);
        // 3. 연관 관계 주인을 통해 FK 제거
        image.changeAccommodation(null);
    }

    // 읽기 전용 컬렉션 제공
    public List<AccommodationImage> getImages() {
        return Collections.unmodifiableList(images);
    }

}
