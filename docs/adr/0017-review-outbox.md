# 0017. 리뷰 이벤트의 Outbox

- 상태: 채택
- 날짜: 2026-11-17

## 맥락
- 리뷰 등록·삭제는 Kafka 이벤트로 숙소 평점을 갱신한다. 지금까지는 커밋 뒤 `@Async` 리스너가 `KafkaTemplate.send`를 호출했다. 리뷰 커밋과 Kafka 발행 사이에 서버가 죽거나 `send`가 실패하면 이벤트가 사라지고, 평점이 영영 어긋난다(숙소 평점은 이 이벤트로만 갱신된다).
- 소비 쪽에는 이미 `@RetryableTopic`, DLT, `event_log`(`event_id` 유니크) 멱등 처리가 있다. 보내는 쪽에 유실 방어가 없었다.
- 이벤트 발행 지점은 결제 확정, 리뷰 등록·삭제, 예약 롤백이다. 결제 확정은 유실돼도 데이터가 어긋나지 않고(타이머 키가 남을 뿐), 예약 롤백은 로그만 남긴다.

## 결정
1. **Outbox는 유실되면 다른 데이터가 영영 어긋나는 이벤트에만 쓴다.** 지금은 리뷰 등록·삭제(숙소 평점)다. 알림, 통계처럼 유실돼도 데이터가 어긋나지 않는 부가 처리는 커밋 뒤 직접 발행(`@Async`)을 유지한다. Outbox 기록이 본 작업과 같은 트랜잭션이라서, 별것 아닌 부가 처리의 기록 실패가 결제 같은 핵심 작업을 실패시키면 안 되기 때문이다.
2. 테이블 `outbox_event`: `event_id`(유니크), `event_type`, `topic`, `message_key`, `payload_type`, `payload`(JSON), `status`(`PENDING`/`SENT`), `attempt_count`, `sent_at`. `(status, id)` 인덱스를 둔다.
3. **기록**: `ReviewService`가 리뷰를 저장하는 같은 트랜잭션에서 `OutboxService.record`로 행을 만든다. 리뷰 저장이 롤백되면 행도 사라진다.
4. **전송**: `OutboxRelayScheduler`가 1초 간격으로 `PENDING` 행을 id 순으로 읽고 한 건씩 `OutboxFacade.relay`를 호출한다.
   - 메서드는 `NOT_SUPPORTED`로 시작해 행을 읽고, 트랜잭션 밖에서 Kafka에 보내 결과를 기다린 뒤(`send(...).get(5s)`), `TransactionTemplate`으로 시도 결과(`attempt_count`, 성공이면 `SENT`)를 기록한다.
   - 전송이 실패하면 `PENDING`으로 두고 첫 실패에서 그 주기를 멈춘다. 숙소 id를 키로 한 파티션 순서를 지키기 위해서다.
5. 이벤트는 적어도 한 번 전달된다. Kafka로 보낸 뒤 `SENT`를 기록하기 전에 서버가 죽으면 한 번 더 나간다. 소비 쪽 `event_log`가 중복을 막으므로 컨슈머는 바꾸지 않는다.
6. 기존 `ReviewEventListener`, `ReviewEventService`, `ReviewCreatedEvent`, `ReviewDeletedEvent`는 없앴다.
7. 코드는 `common/outbox`에 둔다(`event_log`와 같은 공통 위치). 소규모라 층별 패키지로 나누지 않고 같은 패키지에 둔다.

## 이번에 하지 않는 것
- 전송 완료 행 정리(삭제 배치), 최대 재시도 횟수와 폐기(지금은 `PENDING`이 계속 재시도된다), 여러 서버 동시 릴레이 최적화(`SKIP LOCKED`). 중복 전송은 소비 쪽 멱등키로 안전하다.
- 결제 확정 이벤트의 Outbox. 알림이 실제로 붙어도 유실돼도 데이터가 어긋나지 않는다.

## 검토한 대안
- 평점을 이벤트 대신 같은 트랜잭션에서 원자적 증분 쿼리로 갱신: Kafka 없이도 어긋나지 않지만 이 프로젝트에서 Kafka와 Outbox를 쓰는 사례가 사라진다.
- CDC(Debezium)로 binlog를 읽어 발행: 앱 코드 없이 되지만 인프라가 늘어난다.
- 직접 발행을 유지하고 실패를 재시도: 서버가 죽는 경우를 막지 못한다.

## 결과
- 커밋된 리뷰 이벤트는 반드시 Kafka로 나간다(적어도 한 번).
- 리뷰 요청의 응답 시간에 Kafka 전송이 들어가지 않는다(기록만 같은 트랜잭션에서 한다). 평점 반영은 전송 주기(최대 약 1초)만큼 늦어질 수 있다.
- `PENDING` 행이 쌓이는지 볼 지표나 정리 배치는 아직 없다.
