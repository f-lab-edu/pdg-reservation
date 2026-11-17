package com.pdg.reservation.room.facade;

import com.pdg.reservation.accommodation.service.AccommodationService;
import com.pdg.reservation.common.dto.PageInfo;
import com.pdg.reservation.room.dto.RoomDetailInfo;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchInfo;
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

    public PageInfo<RoomSearchInfo> getRooms(Long accommodationId, RoomSearchCondition condition, Pageable pageable) {
        accommodationService.validateExists(accommodationId);
        Page<RoomSearchInfo> page = roomService.search(accommodationId, condition, pageable);
        return PageInfo.from(fillPrices(page, condition));
    }

    // 객실마다 투숙 기간의 총액과 1박 평균가를 채운다. 계산은 RoomPriceService 한 곳에서 한다
    private Page<RoomSearchInfo> fillPrices(Page<RoomSearchInfo> page, RoomSearchCondition condition) {
        List<Long> roomIds = page.getContent().stream().map(RoomSearchInfo::id).toList();
        Map<Long, StayPrice> stayPrices = roomPriceService.calculateStayPrices(
                roomIds, condition.getCheckInDate(), condition.getCheckOutDate());
        return page.map(room -> {
            StayPrice stayPrice = stayPrices.get(room.id());
            return room.withPrice(stayPrice.totalWon(), stayPrice.averagePerNight().doubleValue());
        });
    }

    public RoomDetailInfo getRoom(Long roomId) {
        return RoomDetailInfo.from(roomService.getRoom(roomId));
    }
}
