package com.pdg.reservation.api.v1.members.login;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.member.dto.LoginRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    TestRestTemplate client;

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
    void 가입한_계정으로_로그인하면_액세스_토큰을_반환한다() {
        // Arrange
        SignupRequest signup = SignupRequest.builder()
                .email("user-" + UUID.randomUUID() + "@example.com")
                .password("Passw0rd!")
                .name("홍길동")
                .nickName("nick" + UUID.randomUUID().toString().substring(0, 8))
                .phoneNumber("010-1234-5678")
                .build();
        client.postForEntity("/api/v1/members", signup, Void.class);
        LoginRequest login = LoginRequest.builder()
                .email(signup.getEmail())
                .password(signup.getPassword())
                .build();

        // Act
        ResponseEntity<ApiResponse<LoginResponse>> response = client.exchange(
                post("/api/v1/members/login").body(login),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse data = response.getBody().getData();
        assertThat(data.getAccessToken()).isNotBlank();
        assertThat(data.getNickName()).isEqualTo(signup.getNickName());
    }
}
