package com.pdg.reservation.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;

@TestConfiguration
public class TestContainersConfig {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("reserv")
            .withCommand(
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_general_ci"
            );

    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine")
            .withExposedPorts(6379)
            .withCommand("redis-server", "--notify-keyspace-events", "Ex");

    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");

    static {
        Startables.deepStart(MYSQL, REDIS, KAFKA).join();

        // 스프링 빈 생성 순서와 무관하게 컨텍스트 시작 전에 주소를 넣기 위해 시스템 프로퍼티를 쓴다.
        System.setProperty("spring.datasource.url",
                MYSQL.getJdbcUrl() + "?serverTimezone=Asia/Seoul&useSSL=false&allowPublicKeyRetrieval=true");
        System.setProperty("spring.datasource.username", MYSQL.getUsername());
        System.setProperty("spring.datasource.password", MYSQL.getPassword());
        System.setProperty("spring.data.redis.host", REDIS.getHost());
        System.setProperty("spring.data.redis.port", String.valueOf(REDIS.getMappedPort(6379)));
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
    }
}
