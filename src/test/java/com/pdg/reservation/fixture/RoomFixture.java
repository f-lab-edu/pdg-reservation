package com.pdg.reservation.fixture;

import com.pdg.reservation.accommodation.entity.Accommodation;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.entity.RoomInventory;
import com.pdg.reservation.room.repository.RoomInventoryRepository;
import com.pdg.reservation.room.repository.RoomRepository;
import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.pdg.reservation.fixture.RoomGenerator.generateRoom;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomInventories;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomInventoriesWithNightlyPrices;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomInventory;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomWithImages;
import static com.pdg.reservation.fixture.RoomGenerator.generateRoomWithNonMainImage;

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

    // 이미지(대표 1개 포함)를 가진 객실을 저장하고 돌려준다
    public Room createRoomWithImages(Accommodation accommodation) {
        return roomRepository.save(generateRoomWithImages(accommodation));
    }

    // 대표가 아닌 이미지 1개만 가진 객실을 저장하고 돌려준다
    public Room createRoomWithNonMainImage(Accommodation accommodation) {
        return roomRepository.save(generateRoomWithNonMainImage(accommodation));
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

    // 기본 요금을 지정한 객실을 저장하고 돌려준다
    public Room createRoom(Accommodation accommodation, long basePrice) {
        return roomRepository.save(generateRoom(accommodation, BigDecimal.valueOf(basePrice)));
    }

    // 객실의 체크인부터 하루씩, 지정한 가격으로 재고를 저장하고 돌려준다. 목록의 null은 가격이 없는 밤이다
    public List<RoomInventory> createRoomInventoriesWithNightlyPrices(Room room, LocalDate checkInDate, List<Long> prices) {
        return roomInventoryRepository.saveAll(generateRoomInventoriesWithNightlyPrices(room, checkInDate, prices));
    }

    // 객실의 하루치 재고 행을 재고 여부를 지정해 저장하고 돌려준다
    public RoomInventory createRoomInventory(Room room, LocalDate date, boolean isStocked) {
        return roomInventoryRepository.save(generateRoomInventory(room, date, isStocked));
    }
}
