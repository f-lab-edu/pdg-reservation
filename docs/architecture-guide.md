# 아키텍처 가이드

코드를 어디에 두고 어떻게 연결할지 정한 규칙이다. 근거와 대안은 [ADR 0015](adr/0015-layer-guide.md)에 있다. 이 문서는 앞으로 새로 쓰는 코드와 손대는 코드가 따른다. 기존 코드는 로드맵 R2-2, R3-1에서 이 가이드에 맞춘다.

- DDD 전부를 적용하지 않는다. 비즈니스 규칙을 도메인 객체에 두는 것, 계층의 의존 방향, 외부 시스템 격리만 가져온다.
- 헥사고날의 포트/어댑터 분리, CQRS, 바운디드 컨텍스트 분리는 하지 않는다.

## 1. 레이어

```
Controller / Listener / Scheduler / Consumer   ← 진입점
        ↓
      Facade                ← 여러 Service 조율, 트랜잭션 경계
        ↓
      Service               ← 한 도메인의 흐름
        ↓
    Repository              ← DB 접근
(엔티티, VO, Enum은 도메인 층: 규칙은 여기에)
```

### 의존 규칙
- 위에서 아래로만 의존한다. 역방향은 금지한다.
- Facade는 Service만 호출한다. Repository는 호출하지 않는다.
- Service는 **자기 도메인의 Repository만** 쓴다. 다른 도메인의 Service나 Repository를 호출하지 않고, Service끼리도 서로 호출하지 않는다(같은 도메인이어도). 여러 도메인이 엮이는 조율은 Facade가 한다.
- Repository끼리 서로 호출하지 않는다. 다만 QueryDSL 쿼리 안에서 다른 테이블을 JOIN하는 것은 허용한다(조회 전용).
- 이렇게 하면 순환 참조가 구조적으로 생기지 않는다.

### 진입점
- Controller, 이벤트 리스너, 스케줄러, Kafka 컨슈머, Redis 만료 리스너는 모두 진입점이고 **Facade만 호출한다.**
- 진입점은 얇게 둔다. 요청이나 이벤트를 풀어서 Facade 메서드 하나를 호출하는 것만 한다. 판단과 조회를 하지 않는다.
- 스케줄러는 한 건당 Facade를 한 번 호출한다(건별로 트랜잭션이 나뉜다).

## 2. 층별 책임

| 층 | 하는 일 | 하지 않는 일 |
|---|---|---|
| Controller | 요청·응답 변환, `@Valid` 형식 검증 | 규칙, 조회 |
| Facade | 여러 Service 호출 순서 조율, 트랜잭션 경계, 응답용 Info 변환, 이벤트 발행(예외 케이스) | 규칙 판단(`if`), Repository 호출 |
| Service | 한 도메인의 흐름: 엔티티 조회 → 엔티티 메서드 호출 → 저장, 이벤트 발행 | 규칙 판단, 다른 도메인 호출 |
| 엔티티·VO | 도메인 규칙, 상태 전이, 자기 데이터로 하는 계산 | DB 조회, 스프링 의존 |
| `XxxDomainService` | 여러 객체에 걸쳐 엔티티나 VO에 넣을 수 없는 규칙 | Repository 주입, 상태, 트랜잭션 |
| Repository | 조회와 저장 | Response 생성, 도메인 계산 |

## 3. 데이터 전달 (DTO)

```
명령: Request ──→ Facade ──→ Service(엔티티 반환) ──→ Facade가 Info로 변환 ──→ Controller가 Response로 변환
조회: Repository(Info로 바로 프로젝션) ──→ Service ──→ Facade(값 보강) ──→ Controller가 Response로 변환
```

- **Command는 만들지 않는다.** Request가 Facade와 Service까지 그대로 내려간다. 대신 Request에 서블릿·HTTP 타입(`HttpServletRequest` 등)을 넣지 않는다.
- Service는 엔티티를 돌려준다. Facade가 필요하면 다른 Service에 엔티티를 넘긴다. 조회 전용 쿼리는 엔티티를 거치지 않고 Repository가 Info로 바로 만들어 돌려준다.
- **모든 API는 Info를 거친다.** Facade는 Info를 반환하고, Response는 Controller가 Info에서 만든다(`Response.from(Info)`). 예외는 없다. 엔티티는 Facade 위로 올라가지 않는다.
- 페이지 결과는 Facade가 `PageInfo<Info>`로 반환하고 Controller가 `PageResponse.from(pageInfo, Response::from)`으로 바꾼다.
- 쿼리에서 만들 수 없는 값(검색의 가격, 최저가)은 Facade가 채운다. Info는 불변 `record`이므로 `withXxx(...)`로 새 객체를 만든다.
- Redis 캐시에 저장되는 Info(`AccommodationDetailInfo`, `ReviewSummaryInfo`, `PageInfo`, `ImageInfo`)는 record가 아니라 기본 생성자를 가진 일반 클래스로 둔다. 캐시 직렬화가 최종 클래스(record 포함)에는 타입 정보를 넣지 않아서 역직렬화가 깨진다(`RedisConfig`의 `DefaultTyping.NON_FINAL`).
- 엔티티 생성은 `Request.toEntity`가 아니라 엔티티의 정적 팩토리가 한다.
- 변환 팩토리 이름은 `from`(단일 소스), `of`(여러 값)이다.

## 4. 규칙을 어디에 두나

위에서부터 먼저 해당하는 곳에 둔다.

1. **그 엔티티의 데이터만으로 판단되면 엔티티 메서드.** 예: `Reservation.validateCancelable()`.
2. **상태 판단은 엔티티가 한다.** 상태 비교와 날짜·금액 같은 추가 조건을 한 메서드에 모은다. Enum은 상태 이름과 값만 가지고 판단 로직을 넣지 않는다.
3. **값 하나의 개념은 VO**이다. 단, 아래 둘 중 하나에 해당할 때만 만든다.
   - 값 여러 개가 항상 같이 다닌다(예: 주소).
   - 그 값 자체의 검증, 계산, 변환 로직이 있다(예: `StayPrice`).

   해당하지 않으면 `String`, `BigDecimal`, `Long` 같은 원시 타입으로 둔다. 로직이 생기면 그때 VO로 올린다. VO는 불변이다(`record` 권장).
4. **여러 객체에 걸쳐 어디에도 자연스럽게 못 넣을 때만 `XxxDomainService`.** 엔티티에 넣을 수 있는 것을 Service로 빼지 않는다.

### 엔티티 규칙
- setter를 만들지 않는다. 의미 있는 메서드(`cancel()`, `changeShippingInfo()`)로만 바꾼다.
- 엔티티는 조회하지 않는다. 필요한 데이터는 호출하는 쪽이 인자로 넘긴다(`Room.calculateStayPrice(체크인, 체크아웃, 특별 요금들)`). 행이 많아질 수 있는 데이터는 컬렉션으로 소유하지 않는다.
- **규칙을 어기면 엔티티가 `CustomException(ErrorCode)`을 직접 던진다.** 별도의 도메인 예외 계층은 만들지 않는다.
- 새 `XxxValidator` 클래스는 만들지 않는다. 엔티티 메서드나 `XxxDomainService`를 쓴다.
- Service 메서드에 도메인 상태에 대한 `if`가 생기면 엔티티 메서드로 옮긴다.

### `XxxDomainService`
- 이름은 `XxxDomainService`이고 `{도메인}/service/`에 둔다.
- Repository나 다른 Service를 주입받지 않고, 상태도 갖지 않고, `@Transactional`도 붙이지 않는다. 이미 조회된 엔티티와 VO를 인자로 받아 판단하거나 계산해서 결과를 돌려준다.
- 여러 도메인에 걸친 규칙이면 **Facade가 호출한다.** 한 도메인 안의 규칙이면 그 도메인 Service가 호출한다.
- Facade는 결과를 그대로 쓰거나 넘기기만 한다. 결과를 보고 `if`로 규칙을 판단하지 않는다. 판단이 필요하면 `DomainService`나 엔티티 메서드 안으로 넣는다.

## 5. 트랜잭션

- 트랜잭션 경계는 Facade가 정한다. Service는 Facade 트랜잭션에 합류한다(`REQUIRED`).
- Facade 클래스는 `@Transactional(readOnly = true)`를 기본으로 둔다. **상태를 바꾸는 메서드는 반드시 `@Transactional`을 붙인다**(빠뜨리면 읽기 전용 트랜잭션에 합류해서 저장이 조용히 안 된다).

### 외부 호출(PG 등)이 있는 Facade 메서드
- DB 트랜잭션이 외부 호출을 감쌀 수 없으므로 예외 규칙을 쓴다.
  - 메서드에 `@Transactional(propagation = NOT_SUPPORTED)`를 붙인다. 맨 바깥 Facade에서 클래스의 읽기 전용 기본값을 끄는 용도이고, 이 메서드에서는 트랜잭션이 시작되지 않는다. **안쪽 Service에는 쓰지 않는다**(중단된 트랜잭션도 커넥션을 계속 잡는다).
  - DB 구간만 `TransactionTemplate`으로 묶는다. 읽기 검증, 외부 호출, DB 변경을 구간별로 나눈다.
  - DB 변경은 **한 트랜잭션에 모은다.** 보상이 필요한 것은 외부 시스템 취소 하나뿐이어야 한다.
  - 외부 호출 전의 DB 검증과 쓰기 사이에 상태가 바뀔 수 있으므로, 쓰기 트랜잭션 안에서 상태를 다시 검증한다.
- 주석으로 "외부 호출이 있어서 트랜잭션을 나눈다"고 이유를 적는다.

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED) // 외부 호출(PG)이 있어서 DB 구간만 TransactionTemplate로 묶는다
public PaymentInfo pay(PaymentRequest request) {
    tx.executeWithoutResult(s -> paymentService.validatePayable(request)); // 읽기 검증
    PgResult result = pgService.approve(request);                          // PG 호출(트랜잭션 밖)
    try {
        return tx.execute(s -> {                                           // DB 변경은 한 트랜잭션
            Payment payment = paymentService.save(request, result);
            reservationService.confirm(payment.getReservationId());        // 안에서 상태를 다시 검증
            return PaymentInfo.from(payment);
        });
    } catch (Exception e) {
        pgService.cancel(result);                                          // 보상은 외부 시스템 취소 하나
        throw e;
    }
}
```

- Facade가 읽기 전용 구간에 쓰는 `TransactionTemplate`은 `setReadOnly(true)`인 별도 빈으로 둔다.
- 같은 클래스 안에서 `@Transactional` 메서드를 서로 호출하면 프록시를 거치지 않아 트랜잭션이 열리지 않는다. Facade가 Service 메서드를 하나씩 호출해서 잇는다.

## 6. 이벤트

- **발행**
  - 개별 Service에서 일어난 사실의 이벤트는 **그 Service가 발행한다.** 예: 회원 가입 Service가 `MemberJoinedEvent`를 발행하고 알림 리스너가 받는다.
  - 여러 Service의 작업이 모두 끝나야 성립하는 이벤트(단계마다 트랜잭션이 나뉘는 외부 호출 유스케이스)는 **Facade가 발행한다.** 이름은 "유스케이스 완료"의 의미가 드러나게 짓는다. 트랜잭션 밖에서 발행하면 `AFTER_COMMIT` 리스너가 실행되지 않으므로, 템플릿 구간 안에서 발행하거나 구독 측에 `fallbackExecution`을 둔다.
  - 같은 사실을 두 곳에서 발행하지 않는다.
  - 엔티티는 이벤트를 발행하지 않는다.
- **함께 성공해야 하는 후속 처리**(가입 후 상품 지급)는 이벤트가 아니라 Facade가 Service를 순서대로 호출한다.
- **처리**
  - 외부 시스템을 부르는 부가 처리(Kafka 발행, 알림)는 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("용도별 풀")`을 쓴다. Spring 이벤트는 기본이 동기라서 `@Async`가 없으면 리스너가 끝나야 응답이 나간다.
  - 스레드 풀은 용도별로 나눠 `AsyncConfig`에 이름으로 정의한다(Kafka 발행용, 알림용 등). 거부 정책은 풀마다 정한다.
  - 응답 직후 조회에 반영돼야 하는 처리(캐시 무효화 등)는 동기로 두고, 이유를 주석으로 적는다.
- **리스너는 진입점**이라 Facade를 호출한다. 쓰기가 있으면 Facade 메서드에 `@Transactional`을 붙이고, 동기 `AFTER_COMMIT` 리스너라면 `REQUIRES_NEW`를 명시한다(커밋이 끝난 트랜잭션에 합류하면 쓰기가 커밋되지 않을 수 있다).
- **실패 처리**: `@Async`는 호출한 쪽이 예외를 받지 못한다. 리스너 안에서 예외를 잡아 이벤트 id와 함께 로그를 남기고, 전역 `AsyncUncaughtExceptionHandler`를 등록한다. 이벤트 유실은 `@Async`만으로 막을 수 없다. 중요한 이벤트는 Outbox(로드맵 R5-6)로 보강한다.
- 이벤트의 발행자와 구독자는 문서(이벤트 카탈로그)에 적는다.

## 7. Repository와 외부 시스템

- Repository는 Spring Data JPA 인터페이스를 그대로 쓴다. 도메인 쪽 인터페이스와 인프라 쪽 구현으로 쪼개지 않는다.
- 복잡하거나 동적인 조회는 QueryDSL Custom/Impl로 쓴다. 집합 단위로 하는 판정(가용성 같은 `EXISTS`)은 쿼리에 두고, 도메인 계산(가격)은 쿼리에 넣지 않는다.
- **Repository는 Response를 돌려주지 않는다.** 엔티티나 조회 전용 Info 프로젝션(`@QueryProjection`)을 돌려준다. 컬렉션 인자는 Service는 `List`, Repository는 `Collection`을 받는다.
- 외부 시스템(PG, 알림 등)은 인터페이스와 구현을 나눈다. 호출하는 쪽은 인터페이스에만 의존한다. 지금의 `PgClient`(운영 `MockPgClient`, 테스트 `FakePgClient`)가 이 방식이다.

## 8. 락

- 이중 예약 같은 정합성은 **DB 제약이 우선**이다(ADR 0013). 락으로 대신하지 않는다.
- 분산 락(`@DistributedLock`)은 PG 호출이 낀 결제·취소와 캐시 갱신에만 쓴다.
- 락 애노테이션은 트랜잭션보다 바깥에서 동작해야 한다(락을 먼저 잡고 트랜잭션을 시작). Facade 메서드에 쓸 때 순서를 확인한다.

## 9. 패키지 구조

도메인별 최상위 패키지 안에 층별 패키지를 둔다. 기존 구조에 `facade/`를 추가한다.

```
com.pdg.reservation
└── {도메인}                 # member, accommodation, room, reservation, payment, review
    ├── controller
    ├── facade               # Facade
    ├── service              # Service, XxxDomainService
    ├── repository           # Repository, QueryDSL Custom/Impl
    ├── entity               # 엔티티, VO
    ├── dto                  # Request, Response, Info
    ├── enums
    ├── event
    └── listener             # 진입점: 이벤트 리스너
```

- 스케줄러와 컨슈머가 생기면 같은 층의 `scheduler/`, `consumer/` 패키지에 둔다.
- `application`/`domain`/`infrastructure`로 나누는 구조 이전은 이 가이드를 코드에 반영한 뒤 별도로 검토한다.

## 10. CQRS는 하지 않는다

- 단일 DB이고 조회가 이미 QueryDSL Custom으로 분리돼 있다. 조회 전용 메서드는 `@Transactional(readOnly = true)`로 충분하다.
- 재검토 신호: Service 클래스가 500줄을 넘을 때, 부하 측정(R4)에서 조회가 병목으로 나올 때.

## 11. 테스트

- 도메인 규칙은 엔티티 단위 테스트로(예: `RoomTest`), 사용자 관점의 동작은 API 통합 테스트로 확인한다. 상세는 [test-guide.md](test-guide.md)를 따른다.

## 12. 아직 가이드와 다른 부분 (R3-1에서 정리)

2026-11-17에 Controller → Facade → Service 구조, 진입점의 Facade 호출, 모든 API의 Info 변환, 엔티티 정적 팩토리, `@Async` 리스너를 코드에 반영했다. 아래는 남은 차이다.

- 같은 도메인 안에서 캐시 Service가 락을 쓰는 읽기 컴포넌트를 호출한다(`AccommodationCacheService` → `AccommodationAtomicReader`, `ReviewCacheService` → `ReviewAtomicReader`).
- 인증 주체에서 꺼낸 `Member` 엔티티를 Controller가 Facade에 넘긴다(`reserve`, `createReview`).
- `ReservationFacade.cancel`은 취소 확정 단계가 실패하면 `CANCELING`(취소 진행 중)으로 남고 자동 복구가 없다(ADR 0016). PG 예외를 모두 거절로 보고 되돌리며, 타임아웃 구분과 선조회 복구는 R5-7에서 한다.
- `ReservationEventListener`는 로그만 남기고 Facade를 호출하지 않는다.
- 이벤트 카탈로그 문서가 아직 없다.
