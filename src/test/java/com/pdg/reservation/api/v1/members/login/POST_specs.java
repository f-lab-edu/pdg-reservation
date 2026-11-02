package com.pdg.reservation.api.v1.members.login;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.member.dto.LoginRequest;
import com.pdg.reservation.member.dto.LoginResponse;
import com.pdg.reservation.member.dto.SignupRequest;
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

import static com.pdg.reservation.fixture.MemberGenerator.generateLoginRequest;
import static com.pdg.reservation.fixture.MemberGenerator.generateLoginRequestWithPassword;
import static com.pdg.reservation.fixture.MemberGenerator.generatePassword;
import static com.pdg.reservation.support.JwtAssertions.conformsToJwtFormat;
import static com.pdg.reservation.support.JwtAssertions.hasSubject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/members/login")
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
    void 가입한_계정으로_로그인하면_JWT_형식의_액세스_토큰을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();

        // Act
        ResponseEntity<ApiResponse<LoginResponse>> response = memberFixture.client().exchange(
                post("/api/v1/members/login").body(generateLoginRequest(member)),
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
        ResponseEntity<ApiResponse<LoginResponse>> response = memberFixture.client().exchange(
                post("/api/v1/members/login").body(generateLoginRequest(member)),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getBody().getData().getAccessToken()).satisfies(hasSubject(member.getEmail()));
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_401과_AUTH_003을_반환한다() {
        // Arrange
        SignupRequest member = memberFixture.createMember();
        LoginRequest request = generateLoginRequestWithPassword(member, generatePassword());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/login").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-003");
    }

    @Test
    void 가입하지_않은_이메일로_로그인하면_401과_AUTH_003을_반환한다() {
        // Arrange
        LoginRequest request = generateLoginRequest();

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members/login").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTH-003");
    }
}
