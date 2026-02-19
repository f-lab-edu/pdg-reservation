package com.pdg.reservation.accommodation.dto;

import com.pdg.reservation.accommodation.entity.AccommodationImage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageResponse {

    private final Long id;
    private final String url;
    private final boolean isMain;
    private final int sortOrder;

    public static ImageResponse from(AccommodationImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .url(image.getUrl())
                .isMain(image.isMain())
                .sortOrder(image.getSortOrder())
                .build();
    }


}
