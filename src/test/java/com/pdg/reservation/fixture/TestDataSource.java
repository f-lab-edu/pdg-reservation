package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.enums.AccommodationType;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.pdg.reservation.accommodation.enums.AccommodationType.HOTEL;
import static com.pdg.reservation.fixture.MemberGenerator.generateEmailOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateNameOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateNickNameOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generatePasswordOfLength;
import static com.pdg.reservation.fixture.ReviewGenerator.generateContentOfLength;
import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequestWithContent;
import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequestWithRating;
import static com.pdg.reservation.fixture.ReviewGenerator.generateReviewCreateRequestWithReservationId;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithGuestCount;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithGuestName;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithGuestPhoneNumber;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithPeriod;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithRoomId;
import static com.pdg.reservation.fixture.ReservationGenerator.generateReservationRequestWithTotalPrice;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public final class TestDataSource {

    private TestDataSource() {
    }

    public static Stream<Arguments> invalidEmails() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("형식 오류", "plainaddress")),
                arguments(named("최대 길이 초과 (101자)", generateEmailOfLength(101)))
        );
    }

    public static Stream<Arguments> invalidNames() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("공백만 3자", "   ")),
                arguments(named("최소 길이 미만 (2자)", generateNameOfLength(2))),
                arguments(named("최대 길이 초과 (11자)", generateNameOfLength(11)))
        );
    }

    public static Stream<Arguments> invalidNickNames() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("공백만 3자", "   ")),
                arguments(named("최소 길이 미만 (2자)", generateNickNameOfLength(2))),
                arguments(named("최대 길이 초과 (11자)", generateNickNameOfLength(11)))
        );
    }

    public static Stream<Arguments> invalidPhoneNumbers() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("하이픈 없음", "01012345678")),
                arguments(named("허용하지 않는 앞자리 (011)", "011-1234-5678")),
                arguments(named("가운데 자리 최소 길이 미만 (2자리)", "010-12-5678")),
                arguments(named("가운데 자리 최대 길이 초과 (5자리)", "010-12345-5678")),
                arguments(named("끝 자리 최소 길이 미만 (3자리)", "010-1234-567")),
                arguments(named("끝 자리 최대 길이 초과 (5자리)", "010-1234-56789")),
                arguments(named("숫자가 아닌 문자 포함", "010-12a4-5678")),
                arguments(named("앞에 공백", " 010-1234-5678"))
        );
    }

    public static Stream<Arguments> invalidPasswords() {
        return Stream.of(
                arguments(named("null", null)),
                arguments(named("빈 문자열", "")),
                arguments(named("공백", " ")),
                arguments(named("최소 길이 미만 (7자)", generatePasswordOfLength(7))),
                arguments(named("최대 길이 초과 (21자)", generatePasswordOfLength(21))),
                arguments(named("영문 없음", "12345678!@")),
                arguments(named("숫자 없음", "Abcdefgh!@")),
                arguments(named("특수문자 없음", "Abcdefgh12")),
                arguments(named("한글 포함", "Abcd123!한글가")),
                arguments(named("허용되지 않는 특수문자", "Abcd123^xyz")),
                arguments(named("중간에 공백", "Abcd 123!xyz"))
        );
    }

    public static Stream<Arguments> invalidAccommodationSearchUris() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);
        return Stream.of(
                arguments(named("도시 null", searchUri(null, HOTEL, checkIn, checkOut, 1))),
                arguments(named("도시 빈 문자열", searchUri("", HOTEL, checkIn, checkOut, 1))),
                arguments(named("도시 공백", searchUri(" ", HOTEL, checkIn, checkOut, 1))),
                arguments(named("도시 최대 길이 초과 (51자)", searchUri("가".repeat(51), HOTEL, checkIn, checkOut, 1))),
                arguments(named("유형 null", searchUri("서울", null, checkIn, checkOut, 1))),
                arguments(named("체크인 null", searchUri("서울", HOTEL, null, checkOut, 1))),
                arguments(named("체크인 과거", searchUri("서울", HOTEL, LocalDate.now().minusDays(1), checkOut, 1))),
                arguments(named("체크아웃 null", searchUri("서울", HOTEL, checkIn, null, 1))),
                arguments(named("체크아웃이 체크인 이전", searchUri("서울", HOTEL, checkIn, checkIn.minusDays(1), 1))),
                arguments(named("인원 null", searchUri("서울", HOTEL, checkIn, checkOut, null))),
                arguments(named("인원 최소 미만 (0명)", searchUri("서울", HOTEL, checkIn, checkOut, 0))),
                arguments(named("인원 최대 초과 (101명)", searchUri("서울", HOTEL, checkIn, checkOut, 101)))
        );
    }

    // 숙소 검색 URI를 만든다. 값이 null인 파라미터는 URI에서 뺀다
    private static URI searchUri(String city, AccommodationType type, LocalDate checkIn, LocalDate checkOut, Integer capacity) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("city", city);
        parameters.put("type", type);
        parameters.put("checkInDate", checkIn);
        parameters.put("checkOutDate", checkOut);
        parameters.put("capacity", capacity);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/accommodations");
        parameters.forEach((name, value) -> {
            if (value != null) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().encode().toUri();
    }

    public static Stream<Arguments> invalidReservationRequests() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        return Stream.of(
                arguments(named("객실 id null", generateReservationRequestWithRoomId(null))),
                arguments(named("체크인 null", generateReservationRequestWithPeriod(null, checkIn.plusDays(2)))),
                arguments(named("체크인 과거", generateReservationRequestWithPeriod(LocalDate.now().minusDays(1), checkIn.plusDays(2)))),
                arguments(named("체크아웃 null", generateReservationRequestWithPeriod(checkIn, null))),
                arguments(named("체크아웃이 체크인 이전", generateReservationRequestWithPeriod(checkIn, checkIn.minusDays(1)))),
                arguments(named("체크아웃이 체크인과 같은 날 (0박)", generateReservationRequestWithPeriod(checkIn, checkIn))),
                arguments(named("인원 null", generateReservationRequestWithGuestCount(null))),
                arguments(named("인원 최소 미만 (0명)", generateReservationRequestWithGuestCount(0))),
                arguments(named("인원 최대 초과 (101명)", generateReservationRequestWithGuestCount(101))),
                arguments(named("예약자 이름 null", generateReservationRequestWithGuestName(null))),
                arguments(named("예약자 이름 빈 문자열", generateReservationRequestWithGuestName(""))),
                arguments(named("예약자 이름 공백", generateReservationRequestWithGuestName(" "))),
                arguments(named("예약자 이름 최대 길이 초과 (51자)", generateReservationRequestWithGuestName("가".repeat(51)))),
                arguments(named("연락처 null", generateReservationRequestWithGuestPhoneNumber(null))),
                arguments(named("연락처 빈 문자열", generateReservationRequestWithGuestPhoneNumber(""))),
                arguments(named("연락처 공백", generateReservationRequestWithGuestPhoneNumber(" "))),
                arguments(named("연락처 형식 오류 (하이픈 포함)", generateReservationRequestWithGuestPhoneNumber("010-1234-5678"))),
                arguments(named("연락처 최소 길이 미만 (8자리)", generateReservationRequestWithGuestPhoneNumber("01234567"))),
                arguments(named("연락처 최대 길이 초과 (12자리)", generateReservationRequestWithGuestPhoneNumber("010123456789"))),
                arguments(named("총액 null", generateReservationRequestWithTotalPrice(null)))
        );
    }

    public static Stream<Arguments> invalidReviewCreateRequests() {
        return Stream.of(
                arguments(named("예약 id null", generateReviewCreateRequestWithReservationId(null))),
                arguments(named("평점 null", generateReviewCreateRequestWithRating(null))),
                arguments(named("평점 최소 미만 (0.4)", generateReviewCreateRequestWithRating(new BigDecimal("0.4")))),
                arguments(named("평점 최대 초과 (5.1)", generateReviewCreateRequestWithRating(new BigDecimal("5.1")))),
                arguments(named("평점 소수 둘째 자리 (4.55)", generateReviewCreateRequestWithRating(new BigDecimal("4.55")))),
                arguments(named("내용 null", generateReviewCreateRequestWithContent(null))),
                arguments(named("내용 빈 문자열", generateReviewCreateRequestWithContent(""))),
                arguments(named("내용 공백", generateReviewCreateRequestWithContent(" "))),
                arguments(named("내용 최소 길이 미만 (9자)", generateReviewCreateRequestWithContent(generateContentOfLength(9)))),
                arguments(named("내용 최대 길이 초과 (301자)", generateReviewCreateRequestWithContent(generateContentOfLength(301))))
        );
    }

    // 작성되어야 하는 평점·내용의 경계 값. 인자는 (평점, 내용)이다
    public static Stream<Arguments> validReviewBoundaryInputs() {
        return Stream.of(
                argumentSet("평점 최소 0.5", new BigDecimal("0.5"), generateContentOfLength(50)),
                argumentSet("평점 최대 5.0", new BigDecimal("5.0"), generateContentOfLength(50)),
                argumentSet("내용 최소 길이 10자", new BigDecimal("4.0"), generateContentOfLength(10)),
                argumentSet("내용 최대 길이 300자", new BigDecimal("4.0"), generateContentOfLength(300))
        );
    }

    public static Stream<Arguments> invalidReservationSearchUris() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        return Stream.of(
                arguments(named("객실명 최대 길이 초과 (51자)", reservationSearchUri("가".repeat(51), null, null, null, null))),
                arguments(named("숙소명 최대 길이 초과 (51자)", reservationSearchUri(null, "가".repeat(51), null, null, null))),
                arguments(named("체크아웃이 체크인 이전", reservationSearchUri(null, null, null, checkIn, checkIn.minusDays(1)))),
                arguments(named("체크아웃이 체크인과 같은 날", reservationSearchUri(null, null, null, checkIn, checkIn))),
                arguments(named("상태 값 오류", reservationSearchUri(null, null, "INVALID", null, null)))
        );
    }

    // 예약 검색 URI를 만든다. 값이 null인 파라미터는 URI에서 뺀다
    private static URI reservationSearchUri(String roomName, String accommodationName, String status, LocalDate checkIn, LocalDate checkOut) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("roomName", roomName);
        parameters.put("accommodationName", accommodationName);
        parameters.put("status", status);
        parameters.put("checkInDate", checkIn);
        parameters.put("checkOutDate", checkOut);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/reservations/search");
        parameters.forEach((name, value) -> {
            if (value != null) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().encode().toUri();
    }

    public static Stream<Arguments> invalidRoomSearchUris() {
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(2);
        return Stream.of(
                arguments(named("체크인 null", roomSearchUri(null, checkOut, 1))),
                arguments(named("체크인 과거", roomSearchUri(LocalDate.now().minusDays(1), checkOut, 1))),
                arguments(named("체크아웃 null", roomSearchUri(checkIn, null, 1))),
                arguments(named("체크아웃이 체크인 이전", roomSearchUri(checkIn, checkIn.minusDays(1), 1))),
                arguments(named("체크아웃이 체크인과 같은 날", roomSearchUri(checkIn, checkIn, 1))),
                arguments(named("인원 null", roomSearchUri(checkIn, checkOut, null))),
                arguments(named("인원 최소 미만 (0명)", roomSearchUri(checkIn, checkOut, 0))),
                arguments(named("인원 최대 초과 (101명)", roomSearchUri(checkIn, checkOut, 101)))
        );
    }

    // 객실 검색 URI를 만든다. 값이 null인 파라미터는 URI에서 뺀다. 입력 검증이 먼저 일어나므로 숙소 id는 존재하지 않아도 된다
    private static URI roomSearchUri(LocalDate checkIn, LocalDate checkOut, Integer capacity) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("checkInDate", checkIn);
        parameters.put("checkOutDate", checkOut);
        parameters.put("capacity", capacity);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/v1/accommodations/1/rooms");
        parameters.forEach((name, value) -> {
            if (value != null) {
                builder.queryParam(name, value);
            }
        });
        return builder.build().encode().toUri();
    }
}
