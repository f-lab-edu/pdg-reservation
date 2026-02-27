package com.pdg.reservation.room.entity;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
import com.pdg.reservation.accommodation.enums.BedType;
import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
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
        image.changeRoom(this);
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
        image.changeRoom(null);
    }

    // 읽기 전용 컬렉션 제공
    public List<AccommodationImage> getImages() {
        return Collections.unmodifiableList(images);
    }



}
