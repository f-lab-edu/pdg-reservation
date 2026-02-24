package com.pdg.reservation.common.aop;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.common.exception.CustomException;
import com.pdg.reservation.common.exception.enums.ErrorCode;
import com.pdg.reservation.common.util.CustomSpringELParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@Order(1) // @Transactional(보통 최하위 순위)보다 먼저 실행되도록 순서 강제
@RequiredArgsConstructor
public class DistributedLockAspect {

    private static final String REDISSON_LOCK_PREFIX = "LOCK:";
    private final RedissonClient redissonClient;

    @Around("@annotation(com.pdg.reservation.common.annotation.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        // 1. SpEL을 파싱하여 동적인 락 키 생성 (예: LOCK:room:1)
        String dynamicKey = (String) CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        String lockKey = REDISSON_LOCK_PREFIX + dynamicKey;

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. 락 획득 시도
            boolean isLocked = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!isLocked) {
                log.warn("[DistributedLock] 락 획득 실패 - Key: {}", lockKey);
                throw new CustomException(ErrorCode.RESERVE_LOCK_ACQUISITION_FAILED);
            }

            log.info("[DistributedLock] 락 획득 성공 - Key: {}", lockKey);
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            log.error("[DistributedLock] 락 획득 중 인터럽트 발생 - Key: {}", lockKey, e);
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.COMMON_INTERNAL_SERVER_ERROR, e);
        } finally {
            // 3. 안전한 락 해제 (트랜잭션 커밋 완료 후 실행됨)
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[DistributedLock] 락 반납 완료 - Key: {}", lockKey);
            }
        }
    }
}