package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.RoomGenerator.generateRoom;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomInventories;

@Component
public class RoomFixture {

    private final TestRestTemplate client;
    private final RoomRepository roomRepository;
    private final RoomInventoryRepository roomInventoryRepository;

    public RoomFixture(ApiClient apiClient,
                       RoomRepository roomRepository,
                       RoomInventoryRepository roomInventoryRepository) {
        this.client = apiClient.rest();
        this.roomRepository = roomRepository;
        this.roomInventoryRepository = roomInventoryRepository;
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }

    // 숙소에 속한 객실을 저장하고 돌려준다
    public Room createRoom(Accommodation accommodation) {
        return roomRepository.save(generateRoom(accommodation));
    }

    // 숙소에 속한 객실을 저장하고, 체크인~체크아웃 전날까지 재고가 있는 날짜별 재고도 저장한 뒤 객실을 돌려준다
    public Room createRoomWithInventories(Accommodation accommodation, LocalDate checkInDate, LocalDate checkOutDate) {
        Room room = createRoom(accommodation);
        createRoomInventories(room, checkInDate, checkOutDate);
        return room;
    }

    // 객실의 체크인~체크아웃 전날까지 재고가 있는 날짜별 재고를 저장하고 돌려준다
    public List<RoomInventory> createRoomInventories(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        return roomInventoryRepository.saveAll(generateRoomInventories(room, checkInDate, checkOutDate));
    }
}
