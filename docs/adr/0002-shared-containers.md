# 0002. 컨테이너 공유와 주소 주입

- 상태: 채택
- 날짜: 2026-11-01

## 맥락
- 테스트는 MySQL, Redis, Kafka를 Testcontainers로 띄운다. 테스트마다 새로 띄우면 너무 느리다.
- 컨테이너 주소(랜덤 포트)를 스프링 설정에 넣어야 한다. Redisson은 `spring.data.redis.host/port`를 `@Value`로 읽는다.

## 결정
- 컨테이너 3개를 `static` 필드로 정의하고 `static` 블록에서 `Startables.deepStart(...)`로 한 번만 병렬 기동한다. 테스트 전체가 공유한다.
- 접속 주소는 같은 `static` 블록에서 `System.setProperty`로 넣는다(`spring.datasource.*`, `spring.data.redis.*`, `spring.kafka.bootstrap-servers`).
- 이미지: MySQL `mysql:8.0`(DB `reserv`, `utf8mb4`, `utf8mb4_general_ci`, `serverTimezone=Asia/Seoul`), Redis `redis:7.4-alpine`(`--notify-keyspace-events Ex`, 키 만료 리스너 때문), Kafka는 ADR 0004. MySQL은 데이터 디렉터리를 tmpfs에 올리고 `--skip-log-bin`을 붙여 기동을 줄였다(약 15.3초에서 7.8초, `docs/test-speed.md`).
- `application-test.yml`에는 접속 주소를 넣지 않는다.

## 검토한 대안
- `DynamicPropertyRegistrar` 빈으로 주소 주입: 처음 시도했을 때 `Failed to determine suitable jdbc url`이 났다. DataSource가 웹 서버 기동 단계에서 먼저 만들어지는 것이 스택 트레이스에 보였다. 순서 문제로 추정하지만 다시 확인하지는 않았다.
- `@ServiceConnection`: JDBC URL에 `serverTimezone` 같은 옵션을 붙이기 어렵고, Redisson이 프로퍼티를 읽어서 Redis에는 맞지 않는다. 주입 방식을 하나로 맞추기 위해 쓰지 않았다.
- `@DynamicPropertySource`: `@Import`한 설정 클래스에서는 동작하지 않는다.
- `@Testcontainers`/`@Container`: JUnit 확장이 테스트 클래스가 끝날 때 컨테이너를 멈춘다. 스프링 컨텍스트는 설정이 같은 테스트 클래스끼리 캐시로 재사용되므로, 다음 클래스는 이미 멈춘 컨테이너에 연결하려다 실패한다. 여러 테스트 클래스가 컨테이너를 공유하는 이 프로젝트에는 맞지 않아 쓰지 않았다.
- 컨테이너 재사용(`withReuse(true)` + `~/.testcontainers.properties`의 `testcontainers.reuse.enable=true`): 2026-11-03에 `GET_specs`로 측정했다. 컨테이너 기동이 약 15초에서 1초로 줄고 전체 소요가 50~53초에서 36~37초로 줄었다. 하지만 컨테이너가 테스트 뒤에도 남아 직접 지워야 하고, 정리하지 않는 Kafka의 토픽·오프셋이 실행 사이에 이월되며, Testcontainers도 CI에는 맞지 않는 실험 기능이라고 본다. 이득(약 15초)이 대가보다 작다고 판단해 쓰지 않았다. 전체 테스트가 길어져 자주 기다리게 되면 다시 검토한다.

## 결과
- 시스템 프로퍼티가 테스트 JVM 전체에 남는다.
- `static` 블록은 스프링이 설정 클래스를 CGLIB 프록시로 만들 때(빈 생성 전) 실행되는 것이 로그로 확인됐다. 이 시점에 기대고 있어서 설정 클래스에 `proxyBeanMethods = false`를 붙이면 실행 시점이 달라질 수 있다.
- 컨테이너 정리는 Testcontainers의 Ryuk가 JVM 종료 후 한다.
- 주소가 `application-test.yml`에 없으므로, 컨테이너 설정 없이 돌리면 연결 실패로 끝나고 개발 DB는 건드리지 않는다.
- 컨테이너는 테스트 실행 한 번 동안만 산다. 다음 실행에서는 스프링 컨텍스트와 컨테이너를 처음부터 다시 띄운다(tmpfs 적용 후 스프링 기동은 컨테이너 기동을 포함해 약 25초. 측정은 `docs/test-speed.md`).
