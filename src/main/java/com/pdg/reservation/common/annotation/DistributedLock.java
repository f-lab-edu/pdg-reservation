package com.pdg.reservation.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {


    // 락의 이름 (SpEL 표현식 사용 가능) 예: "'room:' + #roomId"
    String key();

    // 시간 단위 (기본: 초)
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 락을 기다리는 시간 (기본: 5초)
     * 락 획득을 위해 기다리는 최대 시간
     */
    long waitTime() default 5L;

    /**
     * 락 임대 시간 (기본: 3초)
     * 락을 획득한 후 보유하는 최대 시간 (데드락 방지용)
     */
    long leaseTime() default 3L;
}