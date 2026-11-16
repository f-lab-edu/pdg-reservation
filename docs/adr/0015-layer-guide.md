# 0015. 레이어와 코드 배치 가이드

- 상태: 채택. [ADR 0014](0014-layer-rules.md)(임시)를 대체한다.
- 날짜: 2026-11-16

## 맥락
- ADR 0014는 ERD 수정(R2-1) 동안 쓰는 임시 규칙이었다. 그 사이 R2-2(레이어, Facade, 트랜잭션 경계)를 정하기 위해 DDD 참고 자료를 읽고, 이 프로젝트에 맞게 어디까지 받아들일지 정했다.
- 지금 코드는 Controller가 Service를 직접 부르고, Service끼리 서로 호출하고(취소 Service가 결제·예약 Service를 호출), 규칙이 Service와 `ReservationValidator`에 흩어져 있다. 이벤트 리스너는 Service를 직접 부르고 `@Async`는 쓰이지 않는다.

## 결정
상세 규칙은 [architecture-guide.md](../architecture-guide.md)에 있다. 핵심만 적는다.

1. **레이어는 Controller → Facade → Service → Repository이다.** Facade는 Service만, Service는 자기 도메인의 Repository만 호출한다. Service끼리, 다른 도메인의 Service와 Repository를 호출하지 않는다. 조율은 Facade가 한다. 순환 참조가 구조적으로 생기지 않는다.
2. **진입점은 모두 Facade를 호출한다.** Controller, 이벤트 리스너, 스케줄러, 컨슈머는 얇게 두고 Facade 메서드 하나를 호출한다.
3. **DTO는 Request → Facade → Service(엔티티) → Facade가 Info 변환 → Controller가 Response 변환이다.** Command는 만들지 않는다.
4. **규칙은 도메인 객체에 둔다.** 상태 판단은 엔티티가 하고 Enum은 값만 가진다. VO는 "값이 항상 같이 다니거나 자체 로직이 있을 때"만 만든다. 여러 객체에 걸친 규칙만 `XxxDomainService`(Repository 없음, `service/`에 둔다)로 만들고, 여러 도메인에 걸치면 Facade가 호출한다. 규칙 위반은 엔티티가 `CustomException(ErrorCode)`을 던진다.
5. **트랜잭션은 Facade가 정한다.** 클래스는 `@Transactional(readOnly = true)`, 쓰기 메서드는 `@Transactional`이다. 외부 호출(PG)이 있는 Facade 메서드는 `NOT_SUPPORTED`로 시작해 DB 구간만 `TransactionTemplate`으로 한 트랜잭션에 묶고, 보상은 외부 시스템 취소 하나로 한다.
6. **이벤트는 사실이 생긴 Service가 발행한다.** 여러 Service가 모두 끝나야 성립하는 이벤트만 Facade가 발행한다. 외부 시스템을 부르는 부가 처리는 `AFTER_COMMIT` + `@Async("용도별 풀")`이고 실패는 리스너에서 직접 로그로 남긴다.
7. **Repository는 Spring Data JPA를 그대로 쓰고 포트/어댑터로 쪼개지 않는다.** 외부 시스템만 인터페이스와 구현을 나눈다. 정합성은 DB 제약이 우선이고 분산 락은 결제·취소와 캐시에만 쓴다.
8. **CQRS는 하지 않는다.** 재검토 신호는 Service 500줄 초과, 부하 측정에서 조회 병목이다.
9. 패키지는 도메인별로 두고 `facade/`를 추가한다. `application`/`domain`/`infrastructure` 구조로의 이전은 이 가이드를 코드에 반영한 뒤 따로 검토한다.

## 적용 범위
- 새로 쓰는 코드와 손대는 코드에 적용한다. 기존 코드는 로드맵 R2-2, R3-1에서 맞춘다. 현재 코드와 다른 부분은 가이드 마지막 절에 적어 두었다.

## 검토한 대안
- Service가 다른 Service를 호출: 호출 사슬이 길어지고 순환 참조가 생기기 쉽다. Facade 조율로 대신한다.
- Enum이 상태 판단을 가짐(또는 엔티티와 함께 사용): 상태만으로 끝나는 판단이 거의 없고(상태와 날짜·금액을 함께 본다) 같은 상태 조건이 여러 곳에 반복돼 보이면 그때 뽑을 수 있어서 엔티티만 쓰기로 했다. 상태가 많이 늘어나 반복이 문제가 되면 다시 본다.
- Command 객체 도입: 응용 계층이 API 객체에서 완전히 분리되지만 변환 코드가 늘어난다. Request를 그대로 쓰고 Info로 도메인 노출만 막는다.
- Facade 클래스 전체에 `@Transactional` 없이 메서드마다 명시: 쓰기 메서드의 애노테이션 누락이 저장 누락으로 이어지는 점은 같지만 평소 관례와 달라서, 클래스 기본값을 `readOnly = true`로 두고 외부 호출 메서드만 `NOT_SUPPORTED`로 끈다. 누락은 통합 테스트가 잡는다.
- 외부 호출 유스케이스를 위한 Facade 앞 별도 층: 층이 늘고 진입점 규칙에 예외가 생긴다. `TransactionTemplate`으로 같은 Facade 안에서 해결한다.
- 구조 전체를 `application`/`domain`/`infrastructure`로 이전하고 Repository를 포트/어댑터로 분리: 변경량이 크고 동작에서 얻는 이득이 적어 지금은 하지 않는다.
- CQRS(Service/QueryService, Repository/QueryRepository 분리): 단일 DB이고 조회가 이미 QueryDSL Custom으로 분리돼 있어 도입 비용이 더 크다.

## 결과
- 새 코드는 위치와 호출 방향을 논의하지 않고 이 가이드를 따른다. 규칙을 바꿔야 하면 이 ADR과 가이드를 함께 고친다.
- 기존 코드와 새 코드의 스타일이 R2-2, R3-1에서 정리하기 전까지 섞여 있다.
- 쓰기 Facade 메서드의 `@Transactional` 누락, 외부 호출 Facade에서 트랜잭션 밖 이벤트 발행(리스너 미실행), `@Async` 이벤트의 유실은 구현 때 테스트로 확인해야 하는 위험이다.
