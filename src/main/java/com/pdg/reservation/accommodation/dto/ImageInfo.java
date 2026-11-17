package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.AccommodationImage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 이미지 한 장. 숙소 상세 캐시에 들어가는 값의 일부라 JSON 역직렬화가 되도록 기본 생성자를 둔다
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageInfo {

    private Long id;
    private String url;
    private boolean mainImage;
    private int sortOrder;

    public static ImageInfo from(AccommodationImage image) {
        return ImageInfo.builder()
                .id(image.getId())
                .url(image.getUrl())
                .mainImage(image.isMain())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
