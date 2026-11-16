package com.pdg.reservation.room.service;

import com.pdg.reservation.room.dto.AvailableRoom;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomPrice;
import com.pdg.reservation.room.price.StayPrice;
import com.pdg.reservation.room.price.StayPriceCalculator;
import com.pdg.reservation.room.repository.RoomPriceRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 날짜별 예외 요금(room_price)을 읽어 StayPriceCalculator로 가격을 계산한다.
// 예약 검증, 객실 목록, 숙소 검색이 모두 이 서비스를 거쳐 같은 계산 규칙을 쓴다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomPriceService {

    private final RoomRepository roomRepository;
    private final RoomPriceRepository roomPriceRepository;

    // 객실 하나의 투숙 가격(날짜별 가격과 합계, 등급 할인 전)
    public StayPrice calculateStayPrice(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        Map<LocalDate, BigDecimal> overrides = roomPriceRepository
                .findByRoomIdAndPriceDateGreaterThanEqualAndPriceDateLessThan(room.getId(), checkInDate, checkOutDate)
                .stream()
                .collect(Collectors.toMap(RoomPrice::getPriceDate, RoomPrice::getPrice));
        return StayPriceCalculator.calculate(room.getBasePrice(), checkInDate, checkOutDate, overrides);
    }

    // 객실 여러 개의 투숙 가격. 예외 요금은 한 번의 조회로 가져온다
    public Map<Long, StayPrice> calculateStayPrices(Collection<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, BigDecimal> basePrices = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, Room::getBasePrice));
        return calculateStayPrices(basePrices, checkInDate, checkOutDate);
    }

    // 숙소별로 예약 가능한 객실의 1박 평균가(원 단위 반올림) 중 가장 작은 값
    public Map<Long, Long> minAveragePriceByAccommodation(
            Collection<Long> accommodationIds, LocalDate checkInDate, LocalDate checkOutDate, int capacity) {
        if (accommodationIds.isEmpty()) {
            return Map.of();
        }
        List<AvailableRoom> rooms = roomRepository.findAvailableRooms(accommodationIds, checkInDate, checkOutDate, capacity);
        Map<Long, BigDecimal> basePrices = rooms.stream()
                .collect(Collectors.toMap(AvailableRoom::roomId, AvailableRoom::basePrice));
        Map<Long, StayPrice> stayPrices = calculateStayPrices(basePrices, checkInDate, checkOutDate);

        Map<Long, Long> minByAccommodation = new HashMap<>();
        for (AvailableRoom room : rooms) {
            long average = stayPrices.get(room.roomId()).averageWon();
            minByAccommodation.merge(room.accommodationId(), average, Math::min);
        }
        return minByAccommodation;
    }

    private Map<Long, StayPrice> calculateStayPrices(
            Map<Long, BigDecimal> basePrices, LocalDate checkInDate, LocalDate checkOutDate) {
        if (basePrices.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<LocalDate, BigDecimal>> overridesByRoom = roomPriceRepository
                .findByRoomIdInAndPriceDateGreaterThanEqualAndPriceDateLessThan(basePrices.keySet(), checkInDate, checkOutDate)
                .stream()
                .collect(Collectors.groupingBy(
                        roomPrice -> roomPrice.getRoom().getId(),
                        Collectors.toMap(RoomPrice::getPriceDate, RoomPrice::getPrice)));

        return basePrices.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> StayPriceCalculator.calculate(
                        entry.getValue(), checkInDate, checkOutDate,
                        overridesByRoom.getOrDefault(entry.getKey(), Map.of()))));
    }
}
