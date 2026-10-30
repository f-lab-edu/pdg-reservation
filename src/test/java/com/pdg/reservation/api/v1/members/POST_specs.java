package com.pdg.reservation.api.v1.members;

import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    TestRestTemplate client;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    void 올바른_정보로_가입하면_201을_반환한다() {
        // Arrange
        SignupRequest request = SignupRequest.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .password("Passw0rd!")
                .name("홍길동")
                .nickName("nick" + UUID.randomUUID().toString().substring(0, 8))
                .phoneNumber("010-1234-5678")
                .build();

        // Act
        ResponseEntity<Void> response = client.postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
