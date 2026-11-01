package com.pdg.reservation.api.v1.members;

import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.member.repository.MemberRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    TestRestTemplate client;

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    void 올바른_정보로_가입하면_201을_반환한다() {
        // Arrange
        SignupRequest request = memberFixture.signupRequest();

        // Act
        ResponseEntity<Void> response = client.postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 가입하면_입력한_값이_저장된다() {
        // Arrange
        SignupRequest request = memberFixture.signupRequest();

        // Act
        client.postForEntity("/api/v1/members", request, Void.class);

        // Assert
        Member saved = memberRepository.findByEmail(request.getEmail()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(saved.getName()).isEqualTo(request.getName());
            softly.assertThat(saved.getNickName()).isEqualTo(request.getNickName());
            softly.assertThat(saved.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
            softly.assertThat(saved.isActive()).isTrue();
        });
    }

    @Test
    void 가입하면_비밀번호는_인코딩되어_저장된다() {
        // Arrange
        SignupRequest request = memberFixture.signupRequest();

        // Act
        client.postForEntity("/api/v1/members", request, Void.class);

        // Assert
        Member saved = memberRepository.findByEmail(request.getEmail()).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(request.getPassword(), saved.getPassword())).isTrue();
    }
}
