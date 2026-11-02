package com.pdg.reservation.api.v1.members.logout;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/members/logout")
@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
    }

    @Test
    void 유효한_액세스_토큰으로_로그아웃하면_200을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<Void> response = memberFixture.client()
                .postForEntity("/api/v1/members/logout", null, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void 토큰_없이_로그아웃하면_401과_AUTH_001을_반환한다() {
        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/logout").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }
}
