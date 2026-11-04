package com.pdg.reservation.api.v1.reviews.accommodations.accommodationId;

import com.pdg.reservation.accommodation.dto.AccommodationDetailResponse;
import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.common.dto.PageResponse;
import com.pdg.reservation.fixture.AccommodationFixture;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.fixture.ReservationFixture;
import com.pdg.reservation.fixture.ReviewFixture;
import com.pdg.reservation.fixture.ReviewFixture.NewReview;
import com.pdg.reservation.fixture.RoomFixture;
import com.pdg.reservation.reservation.dto.ReservationResponse;
import com.pdg.reservation.review.dto.ReviewCreateResponse;
import com.pdg.reservation.review.dto.ReviewResponse;
import com.pdg.reservation.review.service.ReviewAtomicReader;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RatingAwaiter;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithNickName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.RequestEntity.get;

@DisplayName("GET /api/v1/reviews/accommodations/{accommodationId}")
@IntegrationTest
@Import(TestContainersConfig.class)
class GET_specs {

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    AccommodationFixture accommodationFixture;

    @Autowired
    RoomFixture roomFixture;

    @Autowired
    ReservationFixture reservationFixture;

    @Autowired
    ReviewFixture reviewFixture;

    @Autowired
    RatingAwaiter ratingAwaiter;

    @Autowired
    ApiClient apiClient;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @MockitoSpyBean
    ReviewAtomicReader reviewAtomicReader;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
        apiClient.reset();
    }

    @Test
    void 숙소의_리뷰를_조회하면_200과_리뷰_목록을_반환하고_닉네임은_마스킹한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser(generateSignupRequestWithNickName("남궁민수"));
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.5"))).get(0);
        // 기대값은 저장한 숙소·객실과 리뷰 생성 응답에서 직접 채운다. 닉네임은 첫 글자와 끝 글자만 보인다
        ReviewResponse expected = ReviewResponse.builder()
                .id(review.getId())
                .nickname("남**수")
                .rating(review.getRating())
                .content(review.getContent())
                .accommodationName(accommodation.getName())
                .roomName(room.getName())
                .build();
        // 숙소별 리뷰는 로그인 없이 조회하는 공개 API다
        apiClient.reset();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ReviewResponse> content = response.getBody().getData().getContent();
        assertThat(content)
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(List.of(expected));
        assertThat(content.get(0).getCreatedAt()).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"홍길동,홍*동", "가나다라마바사아자차,가********차"})
    void 가입할_수_있는_최소_길이와_최대_길이의_닉네임도_첫_글자와_끝_글자만_보이게_마스킹한다(String nickName, String maskedNickName) {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser(generateSignupRequestWithNickName(nickName));
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0")));
        apiClient.reset();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getNickname)
                .containsExactly(maskedNickName);
    }

    @Test
    void 다른_숙소의_리뷰와_삭제된_리뷰는_조회되지_않는다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Accommodation otherAccommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(4));
        Room otherRoom = roomFixture.createRoomWithInventories(otherAccommodation, firstDay, firstDay.plusDays(2));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 2);
        ReservationResponse otherReservation = reservationFixture.createCompletedReservation(otherRoom, firstDay, firstDay.plusDays(1));
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "4.0"),
                NewReview.of(reservations.get(1).getReservationId(), "2.0")));
        reviewFixture.createReviewsAndAwaitRating(otherAccommodation.getId(), List.of(
                NewReview.of(otherReservation.getReservationId(), "3.0")));
        // 둘째 리뷰를 지우고 평점이 되돌아올 때까지 기다린다
        reviewFixture.deleteReview(reviews.get(1).getId());
        ratingAwaiter.awaitRatingAverage(accommodation.getId(), new BigDecimal("4.0"));
        apiClient.reset();
        // 남은 리뷰 1건이 한 페이지(size=1)를 채워야 전체 건수 쿼리도 실행된다
        int size = 1;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?size={size}", accommodation.getId(), size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(0).getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 최소_평점_이상의_리뷰만_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다. 최소 평점 3은 3.0(경계)과 5.0을 포함한다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));
        apiClient.reset();
        int minRating = 3;
        // 조건에 맞는 2건이 한 페이지(size=2)를 채워야 전체 건수 쿼리도 실행된다
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?minRating={minRating}&size={size}",
                        accommodation.getId(), minRating, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> page = response.getBody().getData();
        assertThat(page.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(1).getId(), reviews.get(0).getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void 정렬을_HIGH로_하면_평점이_높은_리뷰부터_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));
        apiClient.reset();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?sortType=HIGH", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(1).getId(), reviews.get(0).getId(), reviews.get(2).getId());
    }

    @Test
    void 정렬을_LOW로_하면_평점이_낮은_리뷰부터_조회된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(6));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 3);
        // 평점 3.0, 5.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "3.0"),
                NewReview.of(reservations.get(1).getReservationId(), "5.0"),
                NewReview.of(reservations.get(2).getReservationId(), "1.0")));
        apiClient.reset();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?sortType=LOW", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(2).getId(), reviews.get(0).getId(), reviews.get(1).getId());
    }

    @Test
    void 숙소_리뷰_검색은_첫_페이지를_올바르게_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(10));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 5);
        // 평점 5.0, 4.0, 3.0, 2.0, 1.0 순서로 쓴다
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "5.0"),
                NewReview.of(reservations.get(1).getReservationId(), "4.0"),
                NewReview.of(reservations.get(2).getReservationId(), "3.0"),
                NewReview.of(reservations.get(3).getReservationId(), "2.0"),
                NewReview.of(reservations.get(4).getReservationId(), "1.0")));
        apiClient.reset();
        int page = 0;
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?page={page}&size={size}",
                        accommodation.getId(), page, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> pageResponse = response.getBody().getData();
        // 최근에 쓴 리뷰부터 보이므로 가장 나중에 쓴 2건이 첫 페이지다
        assertThat(pageResponse.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(4).getId(), reviews.get(3).getId());
        assertThat(pageResponse.getTotalElements()).isEqualTo(5);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isLast()).isFalse();
    }

    @Test
    void 숙소_리뷰_검색은_마지막_페이지를_올바르게_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate firstDay = LocalDate.now().plusDays(30);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, firstDay, firstDay.plusDays(10));
        List<ReservationResponse> reservations = reservationFixture.createCompletedReservations(room, firstDay, 5);
        List<ReviewCreateResponse> reviews = reviewFixture.createReviewsAndAwaitRating(accommodation.getId(), List.of(
                NewReview.of(reservations.get(0).getReservationId(), "5.0"),
                NewReview.of(reservations.get(1).getReservationId(), "4.0"),
                NewReview.of(reservations.get(2).getReservationId(), "3.0"),
                NewReview.of(reservations.get(3).getReservationId(), "2.0"),
                NewReview.of(reservations.get(4).getReservationId(), "1.0")));
        apiClient.reset();
        int lastPage = 2;
        int size = 2;

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?page={page}&size={size}",
                        accommodation.getId(), lastPage, size).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PageResponse<ReviewResponse> pageResponse = response.getBody().getData();
        // 가장 먼저 쓴 리뷰 1건이 마지막 페이지다
        assertThat(pageResponse.getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(reviews.get(0).getId());
        assertThat(pageResponse.getTotalElements()).isEqualTo(5);
        assertThat(pageResponse.getTotalPages()).isEqualTo(3);
        assertThat(pageResponse.isLast()).isTrue();
    }

    @Test
    void 존재하지_않는_숙소의_리뷰를_조회하면_404와_ACC_001을_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        long missingId = accommodation.getId() + 1;

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", missingId).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getErrorCode()).isEqualTo("ACC-001");
    }

    @Test
    void 같은_조건으로_다시_조회하면_DB를_거치지_않고_캐시된_첫_페이지를_반환한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> first = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> second = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(second.getBody().getData()).usingRecursiveComparison().isEqualTo(first.getBody().getData());
        verify(reviewAtomicReader, times(1)).getReviewsWithLock(anyLong(), anyInt(), any(), any());
    }

    @Test
    void 조건이_다르면_별도로_캐시한다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        // 기본 조건으로 한 번 조회해 캐시에 담는다
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );

        // Act
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?minRating=3", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?sortType=HIGH", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?size=5", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );

        // Assert
        // 기본, 최소 평점, 정렬, 페이지 크기가 모두 다른 캐시라서 DB를 4번 읽는다
        verify(reviewAtomicReader, times(4)).getReviewsWithLock(anyLong(), anyInt(), any(), any());
    }

    @Test
    void 둘째_페이지부터는_캐시하지_않는다() {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?page=1", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );

        // Act
        memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?page=1", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<PageResponse<ReviewResponse>>>() { }
        );

        // Assert
        verify(reviewAtomicReader, times(2)).getReviewsWithNoLock(anyLong(), anyInt(), any(), any());
    }

    @Test
    void 리뷰를_쓰면_캐시가_무효화되어_같은_조건의_조회에_새_리뷰가_보인다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        LocalDate checkOutDate = checkInDate.plusDays(2);
        Accommodation accommodation = accommodationFixture.createAccommodation();
        Room room = roomFixture.createRoomWithInventories(accommodation, checkInDate, checkOutDate);
        ReservationResponse reservation = reservationFixture.createCompletedReservation(room, checkInDate, checkOutDate);
        // 리뷰가 없는 목록을 먼저 조회해 캐시에 담는다
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> before = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );
        // 숙소 상세도 먼저 조회해 캐시에 담는다. 평점이 바뀐 것이 보이는 시점은 소비자가 버전을 올린 뒤라는 것을 보장한다
        memberFixture.client().exchange(
                get("/api/v1/accommodations/{id}", accommodation.getId()).build(),
                new ParameterizedTypeReference<ApiResponse<AccommodationDetailResponse>>() { }
        );
        // Act
        ReviewCreateResponse review = reviewFixture.createReviewsAndAwaitRating(
                accommodation.getId(), List.of(NewReview.of(reservation.getReservationId(), "4.0"))).get(0);

        // Assert
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> after = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}", accommodation.getId()).build(),
                new ParameterizedTypeReference<>() { }
        );
        assertThat(before.getBody().getData().getContent()).isEmpty();
        assertThat(after.getBody().getData().getContent())
                .extracting(ReviewResponse::getId)
                .containsExactly(review.getId());
        verify(reviewAtomicReader, times(2)).getReviewsWithLock(anyLong(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidAccommodationReviewSearchUris")
    void 검색_조건이_유효하지_않으면_400과_COMMON_005를_반환한다(URI uri) {
        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                get(uri).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void 최소_평점이_허용_범위의_경계_값이어도_조회된다(int minRating) {
        // Arrange
        Accommodation accommodation = accommodationFixture.createAccommodation();

        // Act
        ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> response = memberFixture.client().exchange(
                get("/api/v1/reviews/accommodations/{accommodationId}?minRating={minRating}", accommodation.getId(), minRating).build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
