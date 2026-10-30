package com.pdg.reservation.support;

import com.pdg.reservation.common.cache.bloom.AccommodationBloomFilter;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisCleanUp {

    private final StringRedisTemplate redisTemplate;
    private final AccommodationBloomFilter accommodationBloomFilter;

    public RedisCleanUp(StringRedisTemplate redisTemplate, AccommodationBloomFilter accommodationBloomFilter) {
        this.redisTemplate = redisTemplate;
        this.accommodationBloomFilter = accommodationBloomFilter;
    }

    public void flushAll() {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        // 필터의 크기·오탐률 설정도 같이 지워지므로, 빈 필터를 다시 만들어 둔다.
        accommodationBloomFilter.init();
    }
}
