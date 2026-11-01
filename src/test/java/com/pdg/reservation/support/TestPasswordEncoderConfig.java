package com.pdg.reservation.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestPasswordEncoderConfig {

    // 테스트에서는 BCrypt 강도를 허용되는 최소값(4)으로 낮춰 가입·로그인 속도를 높인다. (ADR 0009)
    private static final int STRENGTH = 4;

    @Bean
    @Primary
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder(STRENGTH);
    }
}
