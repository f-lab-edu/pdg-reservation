package com.pdg.reservation.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 테스트에서 운영 빈을 바꿔 끼우는 빈을 모아 둔다. @IntegrationTest가 가져온다.
@TestConfiguration
public class TestBeanConfig {

    // 테스트에서는 BCrypt 강도를 허용되는 최소값(4)으로 낮춰 가입·로그인 속도를 높인다. (ADR 0009)
    private static final int BCRYPT_STRENGTH = 4;

    @Bean
    @Primary
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    // 운영의 MockPgClient는 지연과 무작위 실패가 있어 쓸 수 없다. 즉시 응답하고 실패를 제어할 수 있는 Fake로 바꾼다.
    @Bean
    @Primary
    FakePgClient fakePgClient() {
        return new FakePgClient();
    }
}
