package com.pdg.reservation.api.v1.members.login;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.member.dto.LoginResponse;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.RedisCleanUp;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.pdg.reservation.support.JwtAssertions.conformsToJwtFormat;
import static com.pdg.reservation.support.JwtAssertions.hasSubject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    TestRestTemplate client;

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
    void 가입한_계정으로_로그인하면_JWT_형식의_액세스_토큰을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();

        // Act
        ResponseEntity<ApiResponse<LoginResponse>> response = client.exchange(
                post("/api/v1/members/login").body(memberFixture.loginRequest(member)),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getAccessToken()).satisfies(conformsToJwtFormat());
    }

    @Test
    void 로그인하면_토큰의_주체는_가입한_이메일이다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();

        // Act
        ResponseEntity<ApiResponse<LoginResponse>> response = client.exchange(
                post("/api/v1/members/login").body(memberFixture.loginRequest(member)),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getAccessToken()).satisfies(hasSubject(member.getEmail()));
    }
}
