# 0002. 컨테이너 공유와 주소 주입

- 상태: 채택
- 날짜: 2026-11-01

## 맥락
- 테스트는 MySQL, Redis, Kafka를 Testcontainers로 띄운다. 테스트마다 새로 띄우면 너무 느리다.
- 컨테이너 주소(랜덤 포트)를 스프링 설정에 넣어야 한다. Redisson은 `spring.data.redis.host/port`를 `@Value`로 읽는다.

## 결정
- 컨테이너 3개를 `static` 필드로 정의하고 `static` 블록에서 `Startables.deepStart(...)`로 한 번만 병렬 기동한다. 테스트 전체가 공유한다.
- 접속 주소는 같은 `static` 블록에서 `System.setProperty`로 넣는다(`spring.datasource.*`, `spring.data.redis.*`, `spring.kafka.bootstrap-servers`).
- 이미지: MySQL `mysql:8.0`(DB `reserv`, `utf8mb4`, `utf8mb4_general_ci`, `serverTimezone=Asia/Seoul`), Redis `redis:7.4-alpine`(`--notify-keyspace-events Ex`, 키 만료 리스너 때문), Kafka는 ADR 0004.
- `application-test.yml`에는 접속 주소를 넣지 않는다.

## 검토한 대안
- `DynamicPropertyRegistrar` 빈으로 주소 주입: 처음 시도했을 때 `Failed to determine suitable jdbc url`이 났다. DataSource가 웹 서버 기동 단계에서 먼저 만들어지는 것이 스택 트레이스에 보였다. 순서 문제로 추정하지만 다시 확인하지는 않았다.
- `@ServiceConnection`: JDBC URL에 `serverTimezone` 같은 옵션을 붙이기 어렵고, Redisson이 프로퍼티를 읽어서 Redis에는 맞지 않는다. 주입 방식을 하나로 맞추기 위해 쓰지 않았다.
- `@DynamicPropertySource`: `@Import`한 설정 클래스에서는 동작하지 않는다.

## 결과
- 시스템 프로퍼티가 테스트 JVM 전체에 남는다.
- `static` 블록은 스프링이 설정 클래스를 CGLIB 프록시로 만들 때(빈 생성 전) 실행되는 것이 로그로 확인됐다. 이 시점에 기대고 있어서 설정 클래스에 `proxyBeanMethods = false`를 붙이면 실행 시점이 달라질 수 있다.
- 컨테이너 정리는 Testcontainers의 Ryuk가 JVM 종료 후 한다.
- 주소가 `application-test.yml`에 없으므로, 컨테이너 설정 없이 돌리면 연결 실패로 끝나고 개발 DB는 건드리지 않는다.
