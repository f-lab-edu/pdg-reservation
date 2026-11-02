package com.pdg.reservation.fixture;

import com.pdg.reservation.member.dto.SignupRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequest;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MemberFixture {

    private final TestRestTemplate client;

    public MemberFixture(Environment environment) {
        this.client = new TestRestTemplate(new RestTemplateBuilder());
        client.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
    }

    // 가입 API로 회원을 만들고, 가입에 쓴 값을 돌려준다
    public SignupRequest createMember() {
        SignupRequest request = generateSignupRequest();
        ResponseEntity<Void> response = client.postForEntity("/api/v1/members", request, Void.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("회원 가입 실패: " + response.getStatusCode());
        }
        return request;
    }
}
