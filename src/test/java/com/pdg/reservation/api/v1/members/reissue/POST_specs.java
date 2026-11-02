package com.pdg.reservation.api.v1.members.reissue;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.member.dto.TokenResponse;
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

import static com.pdg.reservation.support.JwtAssertions.conformsToJwtFormat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/members/reissue")
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
    void 리프레시_쿠키로_재발급하면_200과_새_액세스_토큰을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();
        String refreshToken = memberFixture.issueRefreshToken(member);

        // Act
        ResponseEntity<ApiResponse<TokenResponse>> response = memberFixture.client().exchange(
                post("/api/v1/members/reissue")
                        .header(HttpHeaders.COOKIE, "refreshToken=" + refreshToken)
                        .build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getAccessToken()).satisfies(conformsToJwtFormat());
    }

    @Test
    void 재발급하면_응답_본문에_리프레시_토큰이_없다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();
        String refreshToken = memberFixture.issueRefreshToken(member);

        // Act
        ResponseEntity<String> response = memberFixture.client().exchange(
                post("/api/v1/members/reissue")
                        .header(HttpHeaders.COOKIE, "refreshToken=" + refreshToken)
                        .build(),
                String.class
        );

        // Assert
        assertThat(response.getBody()).doesNotContain("refreshToken");
    }

    @Test
    void 재발급한_뒤_이전_리프레시_토큰으로_재발급하면_401과_JWT_001을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();
        String oldRefreshToken = memberFixture.issueRefreshToken(member);
        memberFixture.reissueRefreshToken(oldRefreshToken);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/reissue")
                        .header(HttpHeaders.COOKIE, "refreshToken=" + oldRefreshToken)
                        .build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("JWT-001");
    }

    @Test
    void 이전_토큰의_재사용이_감지되면_새_토큰도_무효가_되어_401과_JWT_003을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();
        String usedRefreshToken = memberFixture.issueRefreshToken(member);
        String newRefreshToken = memberFixture.reissueRefreshToken(usedRefreshToken);
        memberFixture.reuseRefreshToken(usedRefreshToken);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/reissue")
                        .header(HttpHeaders.COOKIE, "refreshToken=" + newRefreshToken)
                        .build(),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("JWT-003");
    }
}
