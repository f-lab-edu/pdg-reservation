package com.pdg.reservation.fixture;

import com.pdg.reservation.member.dto.LoginRequest;
import com.pdg.reservation.member.dto.SignupRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static com.pdg.reservation.fixture.MemberGenerator.generateEmail;
import static com.pdg.reservation.fixture.MemberGenerator.generateName;
import static com.pdg.reservation.fixture.MemberGenerator.generateNickName;
import static com.pdg.reservation.fixture.MemberGenerator.generatePassword;
import static com.pdg.reservation.fixture.MemberGenerator.generatePhoneNumber;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MemberFixture {

    private final TestRestTemplate client;

    public MemberFixture(Environment environment) {
        this.client = new TestRestTemplate(new RestTemplateBuilder());
        client.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
    }

    // 임의 값으로 채운 유효한 가입 요청 빌더. 테스트가 필요한 필드만 덮어쓴다
    public SignupRequest.SignupRequestBuilder signupRequestBuilder() {
        return SignupRequest.builder()
                .email(generateEmail())
                .password(generatePassword())
                .name(generateName())
                .nickName(generateNickName())
                .phoneNumber(generatePhoneNumber());
    }

    // 임의 값으로 채운 유효한 가입 요청을 만든다
    public SignupRequest signupRequest() {
        return signupRequestBuilder().build();
    }

    // 가입한 회원의 이메일과 비밀번호로 로그인 요청을 만든다
    public LoginRequest loginRequest(SignupRequest member) {
        return LoginRequest.builder()
                .email(member.getEmail())
                .password(member.getPassword())
                .build();
    }

    // 가입 API로 회원을 만들고, 가입에 쓴 값을 돌려준다
    public SignupRequest createMember() {
        SignupRequest request = signupRequest();
        ResponseEntity<Void> response = client.postForEntity("/api/v1/members", request, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("회원 가입 실패: " + response.getStatusCode());
        }
        return request;
    }
}
