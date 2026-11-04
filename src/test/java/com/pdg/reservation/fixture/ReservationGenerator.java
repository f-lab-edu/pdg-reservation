package com.pdg.reservation.fixture;

import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.room.entity.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ReservationGenerator {

    private static final long UNSAVED_ROOM_ID = 1L;
    private static final BigDecimal DEFAULT_TOTAL_PRICE = new BigDecimal("200000");

    private ReservationGenerator() {
    }

    // 객실과 기간, 인원, 총액을 지정하고 예약자 정보는 임의 값으로 채운 올바른 예약 요청을 만든다
    public static ReservationRequest generateReservationRequest(
            Room room, LocalDate checkInDate, LocalDate checkOutDate, int guestCount, BigDecimal totalPrice) {
        return validRequestBuilder()
                .roomId(room.getId())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .guestCount(guestCount)
                .totalPrice(totalPrice)
                .build();
    }

    // 객실 id만 지정하고 나머지는 올바른 임의 값으로 채운 예약 요청을 만든다
    public static ReservationRequest generateReservationRequestWithRoomId(Long roomId) {
        return validRequestBuilder().roomId(roomId).build();
    }

    // 기간(체크인, 체크아웃)만 지정하고 나머지는 올바른 임의 값으로 채운 예약 요청을 만든다
    public static ReservationRequest generateReservationRequestWithPeriod(LocalDate checkInDate, LocalDate checkOutDate) {
        return validRequestBuilder().checkInDate(checkInDate).checkOutDate(checkOutDate).build();
    }

    public static ReservationRequest generateReservationRequestWithGuestCount(Integer guestCount) {
        return validRequestBuilder().guestCount(guestCount).build();
    }

    public static ReservationRequest generateReservationRequestWithGuestName(String guestName) {
        return validRequestBuilder().guestName(guestName).build();
    }

    public static ReservationRequest generateReservationRequestWithGuestPhoneNumber(String guestPhoneNumber) {
        return validRequestBuilder().guestPhoneNumber(guestPhoneNumber).build();
    }

    public static ReservationRequest generateReservationRequestWithTotalPrice(BigDecimal totalPrice) {
        return validRequestBuilder().totalPrice(totalPrice).build();
    }

    // 저장하지 않은 객실 id와 기본 기간(30일 뒤 2박)으로 채운, 항목마다 올바른 값을 가진 builder
    private static ReservationRequest.ReservationRequestBuilder validRequestBuilder() {
        LocalDate checkInDate = LocalDate.now().plusDays(30);
        return ReservationRequest.builder()
                .roomId(UNSAVED_ROOM_ID)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(2))
                .guestCount(1)
                .guestName("예약자-" + UUID.randomUUID().toString().substring(0, 8))
                .guestPhoneNumber(generatePhoneNumber())
                .totalPrice(DEFAULT_TOTAL_PRICE);
    }

    // 010으로 시작하는 임의 11자리 번호를 만든다
    private static String generatePhoneNumber() {
        return "010" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
    }
}
