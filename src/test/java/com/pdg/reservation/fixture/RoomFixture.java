package com.pdg.reservation.fixture;

import com.pdg.reservation.support.ApiClient;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomFixture {

    private final TestRestTemplate client;

    public RoomFixture(ApiClient apiClient) {
        this.client = apiClient.rest();
    }

    // 모든 Fixture와 테스트가 공유하는 클라이언트
    public TestRestTemplate client() {
        return client;
    }
}
