package com.pdg.reservation.room.service;

import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomSpecialPrice;
import com.pdg.reservation.room.price.StayPrice;
import com.pdg.reservation.room.repository.RoomRepository;
import com.pdg.reservation.room.repository.RoomSpecialPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 날짜별 특별 요금(room_special_price)을 읽어 객실(Room)에게 투숙 가격을 계산시킨다. 계산 규칙은 Room이 갖고 있다.
// 예약 검증, 객실 목록, 숙소 검색이 모두 이 서비스를 거쳐 같은 규칙을 쓴다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomPriceService {

    private final RoomRepository roomRepository;
    private final RoomSpecialPriceRepository roomSpecialPriceRepository;

    // 객실 하나의 투숙 가격(날짜별 가격과 합계, 등급 할인 전)
    public StayPrice calculateStayPrice(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        return calculate(List.of(room), checkInDate, checkOutDate).get(room.getId());
    }

    // 객실 여러 개의 투숙 가격
    public Map<Long, StayPrice> calculateStayPrices(List<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate) {
        return calculate(roomRepository.findAllById(roomIds), checkInDate, checkOutDate);
    }

    // 숙소별로 예약 가능한 객실의 1박 평균가(원 단위 반올림) 중 가장 작은 값
    public Map<Long, Long> minAveragePriceByAccommodation(
            List<Long> accommodationIds, LocalDate checkInDate, LocalDate checkOutDate, int capacity) {
        List<Room> rooms = roomRepository.findAvailableRooms(accommodationIds, checkInDate, checkOutDate, capacity);
        Map<Long, StayPrice> stayPrices = calculate(rooms, checkInDate, checkOutDate);

        Map<Long, Long> minAveragePrices = new HashMap<>();
        for (Room room : rooms) {
            long averagePrice = stayPrices.get(room.getId()).averageWon();
            minAveragePrices.merge(room.getAccommodation().getId(), averagePrice, Math::min);
        }
        return minAveragePrices;
    }

    // 객실마다 그 객실의 특별 요금을 넘겨 계산시킨다. 특별 요금은 한 번의 조회로 가져와 객실별로 나눈다
    private Map<Long, StayPrice> calculate(List<Room> rooms, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        Map<Long, List<RoomSpecialPrice>> specialPricesByRoom = roomSpecialPriceRepository
                .findInStay(roomIds, checkInDate, checkOutDate).stream()
                .collect(Collectors.groupingBy(specialPrice -> specialPrice.getRoom().getId()));

        Map<Long, StayPrice> stayPrices = new HashMap<>();
        for (Room room : rooms) {
            List<RoomSpecialPrice> specialPrices = specialPricesByRoom.getOrDefault(room.getId(), List.of());
            stayPrices.put(room.getId(), room.calculateStayPrice(checkInDate, checkOutDate, specialPrices));
        }
        return stayPrices;
    }
}
