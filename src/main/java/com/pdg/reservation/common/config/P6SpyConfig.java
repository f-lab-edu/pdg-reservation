package com.pdg.reservation.common.config;

import com.p6spy.engine.spy.P6SpyOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class P6SpyConfig {
    @PostConstruct
    public void setLogMessageFormat() {
        //프로그래밍 방식으로 포매터 등록
        P6SpyOptions.getActiveInstance().setLogMessageFormat(CustomP6spySqlFormat.class.getName());
    }
}