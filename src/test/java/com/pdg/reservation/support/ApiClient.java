package com.pdg.reservation.support;

import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

// 테스트가 공유하는 API 클라이언트. 순차 실행을 전제로 하고, 기본 Authorization 하나만 상태로 가진다
@Component
public class ApiClient {

    private final TestRestTemplate rest;
    private String defaultAuthorization;

    public ApiClient(Environment environment) {
        this.rest = new TestRestTemplate(new RestTemplateBuilder());
        rest.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));
        // 요청에 Authorization 헤더가 이미 있으면 그 값을 쓰고, 없을 때만 기본값을 붙인다
        rest.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            if (defaultAuthorization != null && !request.getHeaders().containsKey("Authorization")) {
                request.getHeaders().add("Authorization", defaultAuthorization);
            }
            return execution.execute(request, body);
        });
    }

    // 실제로 요청을 보내는 객체
    public TestRestTemplate rest() {
        return rest;
    }

    // 이후 요청에 이 사용자의 토큰이 기본으로 붙게 한다. 다시 부르면 마지막 값이 이긴다
    public void authenticate(String bearer) {
        this.defaultAuthorization = bearer;
    }

    // 테스트가 끝나면 기본 사용자를 비운다
    public void reset() {
        this.defaultAuthorization = null;
    }
}
