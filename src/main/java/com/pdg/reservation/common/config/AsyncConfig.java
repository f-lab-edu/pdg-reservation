package com.pdg.reservation.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

// @Async 리스너가 쓰는 스레드 풀은 용도별로 나눠 이름으로 정의한다(architecture-guide 6절)
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // Kafka 발행 전용. 브로커가 느려도 다른 비동기 처리의 스레드를 잡아먹지 않게 따로 둔다
    @Bean(name = "kafkaEventExecutor")
    public Executor kafkaEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Kafka-Event-");

        // 거절 전략: 큐가 차면 호출한 쓰레드에서 직접 처리 (데이터 유실 방지)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 우아한 종료 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    // void @Async 메서드의 잡히지 않은 예외는 호출한 쪽이 받을 수 없으므로 같은 형식으로 로그를 남긴다
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("[Async] 처리되지 않은 예외 - method: {}", method.getName(), throwable);
        // errorAlertService.sendMessengerAlert(...);
    }
}
