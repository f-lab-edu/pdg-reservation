package com.pdg.reservation.room.facade;

import com.pdg.reservation.accommodation.service.AccommodationService;
import com.pdg.reservation.room.dto.RoomDetailResponse;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import com.pdg.reservation.room.price.StayPrice;
import com.pdg.reservation.room.service.RoomPriceService;
import com.pdg.reservation.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomFacade {

    private final RoomService roomService;
    private final RoomPriceService roomPriceService;
    private final AccommodationService accommodationService;

    public Page<RoomSearchResponse> getRooms(Long accommodationId, RoomSearchCondition condition, Pageable pageable) {
        accommodationService.validateExists(accommodationId);
        Page<RoomSearchResponse> page = roomService.search(accommodationId, condition, pageable);
        fillPrices(page, condition);
        return page;
    }

    // 객실마다 투숙 기간의 총액과 1박 평균가를 채운다. 계산은 RoomPriceService 한 곳에서 한다
    private void fillPrices(Page<RoomSearchResponse> page, RoomSearchCondition condition) {
        List<Long> roomIds = page.getContent().stream().map(RoomSearchResponse::getId).toList();
        Map<Long, StayPrice> stayPrices = roomPriceService.calculateStayPrices(
                roomIds, condition.getCheckInDate(), condition.getCheckOutDate());
        page.getContent().forEach(room -> {
            StayPrice stayPrice = stayPrices.get(room.getId());
            room.updatePrice(stayPrice.totalWon(), stayPrice.averagePerNight().doubleValue());
        });
    }

    public RoomDetailResponse getRoom(Long roomId) {
        return RoomDetailResponse.from(roomService.getRoom(roomId));
    }
}
