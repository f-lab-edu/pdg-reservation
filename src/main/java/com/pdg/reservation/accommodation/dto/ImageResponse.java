package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.AccommodationImage;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageResponse {

    private Long id;
    private String url;
    private boolean mainImage;
    private int sortOrder;

    public static ImageResponse from(ImageInfo info) {
        return ImageResponse.builder()
                .id(info.getId())
                .url(info.getUrl())
                .mainImage(info.isMainImage())
                .sortOrder(info.getSortOrder())
                .build();
    }


}
