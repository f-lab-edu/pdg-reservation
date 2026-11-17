package com.pdg.reservation.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Outbox 릴레이 같은 주기 작업을 켠다
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
