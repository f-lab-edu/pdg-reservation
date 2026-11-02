package com.pdg.reservation.api.v1.members.logout;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.support.ApiClient;
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
import org.springframework.http.HttpHeaders;
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
    ApiClient apiClient;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @Autowired
    RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.flushAll();
        apiClient.reset();
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
    void 로그아웃하면_리프레시_토큰_쿠키가_삭제된다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();

        // Act
        ResponseEntity<Void> response = memberFixture.client()
                .postForEntity("/api/v1/members/logout", null, Void.class);

        // Assert
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith("refreshToken=")
                        .contains("Max-Age=0"));
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

    @Test
    void 깨진_액세스_토큰으로_로그아웃하면_401과_AUTH_001을_반환한다() {
        // Arrange
        String brokenAccessToken = "broken-access-token";

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + brokenAccessToken)
                        .build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-001");
    }

    @Test
    void 로그아웃한_액세스_토큰으로_다시_로그아웃하면_401과_JWT_001을_반환한다() {
        // Arrange
        memberFixture.createMemberThenSetAsDefaultUser();
        memberFixture.logout();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/logout").build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("JWT-001");
    }
}
