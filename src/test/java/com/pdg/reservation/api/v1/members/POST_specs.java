package com.pdg.reservation.api.v1.members;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.fixture.MemberFixture;
import com.pdg.reservation.member.dto.SignupRequest;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.member.repository.MemberRepository;
import com.pdg.reservation.support.ApiClient;
import com.pdg.reservation.support.DatabaseCleanUp;
import com.pdg.reservation.support.IntegrationTest;
import com.pdg.reservation.support.TestContainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.pdg.reservation.fixture.MemberGenerator.generateEmailOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateNameOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateNickNameOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generatePasswordOfLength;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequest;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithEmail;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithName;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithNickName;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithPassword;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequestWithPhoneNumber;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.http.RequestEntity.post;

@DisplayName("POST /api/v1/members")
@IntegrationTest
@Import(TestContainersConfig.class)
class POST_specs {

    @Autowired
    MemberFixture memberFixture;

    @Autowired
    ApiClient apiClient;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        apiClient.reset();
    }

    @Test
    void 올바른_정보로_가입하면_201을_반환한다() {
        // Arrange
        SignupRequest request = generateSignupRequest();

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 가입하면_입력한_값이_저장된다() {
        // Arrange
        SignupRequest request = generateSignupRequest();

        // Act
        memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

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
        SignupRequest request = generateSignupRequest();

        // Act
        memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        Member saved = memberRepository.findByEmail(request.getEmail()).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(passwordEncoder.matches(request.getPassword(), saved.getPassword())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidEmails")
    void 유효하지_않은_이메일이면_400과_COMMON_005를_반환한다(String email) {
        // Arrange
        SignupRequest request = generateSignupRequestWithEmail(email);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 이메일이_최대_길이_100자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithEmail(generateEmailOfLength(100));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 이미_가입된_이메일이면_409와_MEMBER_001을_반환한다() {
        // Arrange
        SignupRequest existing = memberFixture.createMember();
        SignupRequest request = generateSignupRequestWithEmail(existing.getEmail());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("MEMBER-001");
    }

    @Test
    void 이미_사용_중인_닉네임이면_409와_MEMBER_002를_반환한다() {
        // Arrange
        SignupRequest existing = memberFixture.createMember();
        SignupRequest request = generateSignupRequestWithNickName(existing.getNickName());

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo("MEMBER-002");
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidPasswords")
    void 유효하지_않은_비밀번호이면_400과_COMMON_005를_반환한다(String password) {
        // Arrange
        SignupRequest request = generateSignupRequestWithPassword(password);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 비밀번호가_최소_길이_8자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithPassword(generatePasswordOfLength(8));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 비밀번호가_최대_길이_20자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithPassword(generatePasswordOfLength(20));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidNames")
    void 유효하지_않은_이름이면_400과_COMMON_005를_반환한다(String name) {
        // Arrange
        SignupRequest request = generateSignupRequestWithName(name);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 이름이_최소_길이_3자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithName(generateNameOfLength(3));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 이름이_최대_길이_10자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithName(generateNameOfLength(10));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidNickNames")
    void 유효하지_않은_닉네임이면_400과_COMMON_005를_반환한다(String nickName) {
        // Arrange
        SignupRequest request = generateSignupRequestWithNickName(nickName);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 닉네임이_최소_길이_3자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithNickName(generateNickNameOfLength(3));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 닉네임이_최대_길이_10자이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithNickName(generateNickNameOfLength(10));

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @MethodSource("com.pdg.reservation.fixture.TestDataSource#invalidPhoneNumbers")
    void 유효하지_않은_전화번호이면_400과_COMMON_005를_반환한다(String phoneNumber) {
        // Arrange
        SignupRequest request = generateSignupRequestWithPhoneNumber(phoneNumber);

        // Act
        ResponseEntity<ApiResponse<Void>> response = memberFixture.client().exchange(
                post("/api/v1/members").body(request),
                new ParameterizedTypeReference<>() { }
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorCode()).isEqualTo("COMMON-005");
    }

    @Test
    void 전화번호의_가운데_자리가_최소_길이_3자리이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithPhoneNumber("010-123-4567");

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void 전화번호의_가운데_자리가_최대_길이_4자리이면_가입된다() {
        // Arrange
        SignupRequest request = generateSignupRequestWithPhoneNumber("010-1234-5678");

        // Act
        ResponseEntity<Void> response = memberFixture.client().postForEntity("/api/v1/members", request, Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
