package com.pdg.reservation.fixture;

import com.pdg.reservation.common.dto.ApiResponse;
import com.pdg.reservation.member.dto.LoginResponse;
import com.pdg.reservation.member.dto.SignupRequest;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import static com.pdg.reservation.fixture.MemberGenerator.generateLoginRequest;
import static com.pdg.reservation.fixture.MemberGenerator.generateSignupRequest;
import static org.springframework.http.RequestEntity.post;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MemberFixture {

    private final TestRestTemplate client;

    public MemberFixture(Environment environment) {
        this.client = new TestRestTemplate(new RestTemplateBuilder());
        client.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
    }

    // 테스트마다 새로 만들어지는 클라이언트. 테스트는 이 클라이언트로 API를 호출한다
    public TestRestTemplate client() {
        return client;
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

    // 가입한 회원으로 로그인하고 액세스 토큰을 돌려준다
    public String issueAccessToken(SignupRequest member) {
        ResponseEntity<ApiResponse<LoginResponse>> response = client.exchange(
                post("/api/v1/members/login").body(generateLoginRequest(member)),
                new ParameterizedTypeReference<>() { }
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("로그인 실패: " + response.getStatusCode());
        }
        return response.getBody().getData().getAccessToken();
    }

    // 회원을 만들고 로그인해서, 이후 클라이언트의 모든 요청에 그 회원의 토큰이 붙게 한다
    public SignupRequest createMemberThenSetAsDefaultUser() {
        SignupRequest member = createMember();
        setDefaultAuthorization("Bearer " + issueAccessToken(member));
        return member;
    }

    // 요청에 Authorization 헤더가 이미 있으면 그 값을 쓰고, 없을 때만 기본값을 붙인다
    private void setDefaultAuthorization(String authorization) {
        client.getRestTemplate().getInterceptors().add(0, (request, body, execution) -> {
            if (!request.getHeaders().containsKey("Authorization")) {
                request.getHeaders().add("Authorization", authorization);
            }
            return execution.execute(request, body);
        });
    }
}
