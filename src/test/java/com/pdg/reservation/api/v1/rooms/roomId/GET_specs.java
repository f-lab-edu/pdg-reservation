package com.pdg.reservation.api.v1.rooms.roomId;

import com.pdg.reservation.accommodation.dto.ImageResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.accommodation.entity.AccommodationImage;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.room.dto.RoomDetailResponse;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/rooms/{roomId}")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

    @Autowired
    AccommodationFixture accommodationFixture;

    @Autowired
    RoomFixture roomFixture;

    @Autowired
    ApiClient apiClient;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
        apiClient.reset();
    }

    @Test
    void 존재하는_객실을_조회하면_200과_상세를_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoom(accommodation);
        // 기대값은 운영의 from(...)이 아니라 저장한 값에서 직접 채운다. 이미지는 없고 가격은 기본 요금이다
        RoomDetailResponse expected = RoomDetailResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .description(room.getDescription())
                .maxCapacity(room.getMaxCapacity())
                .roomLabel(room.getRoomLabel())
                .bedType(room.getBedType())
                .price(room.getBasePrice())
                .images(List.of())
                .build();

        // Act
        ResponseEntity<ApiResponse<RoomDetailResponse>> response = roomFixture.client().exchange(
                get("/api/v1/rooms/{roomId}", room.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    void 객실_이미지는_정렬_순서대로_반환하고_대표_이미지_표시와_URL이_맞다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithImages(accommodation);
        // 기대값은 저장한 이미지를 sortOrder 순으로 정렬해 직접 채운다 (운영의 from(...)을 쓰지 않는다)
        List<ImageResponse> expected = room.getImages().stream()
                .sorted(Comparator.comparingInt(AccommodationImage::getSortOrder))
                .map(image -> ImageResponse.builder()
                        .id(image.getId())
                        .url(image.getUrl())
                        .mainImage(image.isMain())
                        .sortOrder(image.getSortOrder())
                        .build())
                .toList();

        // Act
        ResponseEntity<ApiResponse<RoomDetailResponse>> response = roomFixture.client().exchange(
                get("/api/v1/rooms/{roomId}", room.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getImages())
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void 존재하지_않는_객실을_조회하면_404와_ROOM_001을_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoom(accommodation);
        long missingId = room.getId() + 1;

        // Act
        ResponseEntity<ApiResponse<Void>> response = roomFixture.client().exchange(
                get("/api/v1/rooms/{roomId}", missingId).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ROOM-001");
    }
}
