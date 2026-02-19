package com.pdg.reservation.room.controller;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.room.dto.RoomSearchCondition;
import com.pdg.reservation.room.dto.RoomSearchResponse;
import com.pdg.reservation.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accommodations/{accommodationId}/rooms")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomSearchResponse>>> getRooms(
            @PathVariable Long accommodationId,
            @Valid RoomSearchCondition condition,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ) {
        return ApiResponse.ok(roomService.getRooms(accommodationId, condition, pageable));
    }

}
