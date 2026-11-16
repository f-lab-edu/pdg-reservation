package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomBlockedDate;
import com.pdg.reservation.room.repository.RoomBlockedDateRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import com.pdg.reservation.room.repository.RoomSpecialPriceRepository;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.RoomGenerator.generateNonReservableRoom;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoom;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomBlockedDate;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomSpecialPrices;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomWithImages;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomWithNonMainImage;

@Component
public class RoomFixture {

    private final TestRestTemplate client;
    private final RoomRepository roomRepository;
    private final RoomBlockedDateRepository roomBlockedDateRepository;
    private final RoomSpecialPriceRepository roomSpecialPriceRepository;

    public RoomFixture(ApiClient apiClient,
                       RoomRepository roomRepository,
                       RoomBlockedDateRepository roomBlockedDateRepository,
                       RoomSpecialPriceRepository roomSpecialPriceRepository) {
        this.client = apiClient.rest();
        this.roomRepository = roomRepository;
        this.roomBlockedDateRepository = roomBlockedDateRepository;
        this.roomSpecialPriceRepository = roomSpecialPriceRepository;
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 숙소에 속한 객실을 저장하고 돌려준다
    public Room createRoom(Accommodation accommodation) {
        return roomRepository.save(generateRoom(accommodation));
    }

    // 예약 불가능한 객실을 저장하고 돌려준다
    public Room createNonReservableRoom(Accommodation accommodation) {
        return roomRepository.save(generateNonReservableRoom(accommodation));
    }

    // 이미지(대표 1개 포함)를 가진 객실을 저장하고 돌려준다
    public Room createRoomWithImages(Accommodation accommodation) {
        return roomRepository.save(generateRoomWithImages(accommodation));
    }

    // 대표가 아닌 이미지 1개만 가진 객실을 저장하고 돌려준다
    public Room createRoomWithNonMainImage(Accommodation accommodation) {
        return roomRepository.save(generateRoomWithNonMainImage(accommodation));
    }

    // 기본 요금을 지정한 객실을 저장하고 돌려준다
    public Room createRoom(Accommodation accommodation, long basePrice) {
        return roomRepository.save(generateRoom(accommodation, BigDecimal.valueOf(basePrice)));
    }

    // 객실의 첫날부터 하루씩 prices의 개수만큼 특별 요금(room_special_price)을 저장한다.
    // 목록의 null은 특별 요금이 없는(기본 요금인) 밤이다
    public void createSpecialPrices(Room room, LocalDate firstDate, List<Long> prices) {
        roomSpecialPriceRepository.saveAll(generateRoomSpecialPrices(room, firstDate, prices));
    }

    // 운영자가 객실의 하루를 막은 상태로 저장한다
    public RoomBlockedDate createBlockedDate(Room room, LocalDate date) {
        return roomBlockedDateRepository.save(generateRoomBlockedDate(room, date));
    }
}
