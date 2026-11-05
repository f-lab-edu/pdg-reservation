package com.pdg.reservation.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

// 같은 일을 여러 요청자가 동시에 시작하게 만드는 테스트 도구
public final class ConcurrentRunner {

    private static final long TIMEOUT_SECONDS = 30;

    private ConcurrentRunner() {
    }

    // inputs의 원소마다 스레드 하나씩 만들어 task를 실행한다. 모든 스레드가 준비된 뒤 한꺼번에 출발하고,
    // 결과는 inputs와 같은 순서로 돌려준다. 결과를 TIMEOUT_SECONDS 안에 받지 못하면 실패한다
    public static <I, R> List<R> runAtOnce(List<I> inputs, Function<I, R> task) {
        int count = inputs.size();
        ExecutorService executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);   // 스레드가 "준비됐다"를 알린다
        CountDownLatch start = new CountDownLatch(1);       // 메인이 "출발"을 알린다
        try {
            List<Future<R>> futures = new ArrayList<>();
            for (I input : inputs) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.apply(input);
                }));
            }

            ready.await();
            start.countDown();

            List<R> results = new ArrayList<>();
            for (Future<R> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("동시 요청 실행 중 실패했다", e);
        } finally {
            executor.shutdownNow();
        }
    }
}
