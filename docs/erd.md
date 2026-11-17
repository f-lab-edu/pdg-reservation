# ERD (현재 엔티티 기준)

엔티티(JPA)의 테이블 구조와 관계를 Mermaid로 그린 것이다. 엔티티 구조가 바뀌면 이 문서를 함께 갱신한다([CLAUDE.md](../CLAUDE.md) 참고).

- 기준: `main`이 아니라 `develop`의 엔티티. 마지막 갱신: 2026-11-17 (`OUTBOX_EVENT` 추가, ADR 0017). 이전: 2026-11-16 (`ROOM_INVENTORY` 제거, `RESERVATION_NIGHT`·`ROOM_BLOCKED_DATE` 추가, ADR 0013 사이클 3~4). 이전: 2026-11-16 (`ROOM_SPECIAL_PRICE` 추가, ADR 0013 사이클 2). 이전: 2026-11-06 (`Reservation`, `Payment`에 `public_id` 추가, ADR 0012).
- 공통: `BaseEntity`를 상속한 테이블은 `created_at`, `updated_at`을 갖는다(아래 그림에서는 생략).
- `PK` 기본키, `FK` 외래키, `UK` 유니크. 따옴표 안은 설명이다.

```mermaid
erDiagram
    MEMBER ||--o{ RESERVATION : "예약한다"
    MEMBER ||--o{ REVIEW : "작성한다"
    ACCOMMODATION ||--o{ ROOM : "가진다"
    ACCOMMODATION ||--o{ RESERVATION : "예약 대상"
    ACCOMMODATION ||--o{ REVIEW : "리뷰 대상"
    ACCOMMODATION ||--o{ ACCOMMODATION_IMAGE : "이미지(숙소)"
    ROOM ||--o{ ACCOMMODATION_IMAGE : "이미지(객실)"
    ROOM ||--o{ ROOM_BLOCKED_DATE : "운영자가 막은 날짜"
    ROOM ||--o{ ROOM_SPECIAL_PRICE : "날짜별 특별 요금"
    ROOM ||--o{ RESERVATION : "예약 대상"
    ROOM ||--o{ RESERVATION_NIGHT : "날짜별 점유"
    RESERVATION ||--o{ RESERVATION_NIGHT : "점유한 밤"
    RESERVATION ||--o| PAYMENT : "결제(1:1)"
    RESERVATION ||--o| REVIEW : "리뷰(1:1)"
    PAYMENT ||--o| PAYMENT_CANCEL_HISTORY : "취소 이력(1:1)"

    MEMBER {
        bigint id PK
        varchar email UK
        varchar nick_name UK
        varchar password
        varchar name
        varchar phone_number
        varchar grade
        varchar role
        boolean is_active
        datetime deleted_at
    }
    ACCOMMODATION {
        bigint id PK
        varchar name
        varchar type
        varchar city "Address 임베디드"
        varchar zipcode "Address 임베디드"
        varchar street "Address 임베디드"
        json amenities
        time check_in_time
        time check_out_time
        decimal rating_average "리뷰 집계"
        decimal total_rating_sum "리뷰 집계"
        bigint review_count "리뷰 집계"
        boolean is_operational
    }
    ROOM {
        bigint id PK
        bigint accommodation_id FK
        varchar name
        decimal base_price
        int max_capacity
        varchar bed_type
        varchar description
        boolean is_reservable
        varchar room_label
    }
    ROOM_SPECIAL_PRICE {
        bigint id PK
        bigint room_id FK "NOT NULL"
        date price_date "room_id와 함께 유니크, 기본 요금과 다른 날짜만"
        decimal price "0보다 큼, 행이 없는 날은 room.base_price"
    }
    ACCOMMODATION_IMAGE {
        bigint id PK
        bigint accommodation_id FK "NULL 가능"
        bigint room_id FK "NULL 가능"
        varchar image_type
        varchar url
        boolean is_main
        int sort_order
    }
    RESERVATION {
        bigint id PK
        binary16 public_id UK "외부 식별자(UUID)"
        bigint member_id FK
        bigint accommodation_id FK
        bigint room_id FK
        date check_in_date
        date check_out_date
        int guest_count
        decimal total_price
        varchar status
        varchar guest_name
        varchar guest_phone_number
        datetime confirmed_at
        datetime canceled_at
        bigint version "낙관적 락"
    }
    ROOM_BLOCKED_DATE {
        bigint id PK
        bigint room_id FK "NOT NULL"
        date blocked_date "room_id와 함께 유니크"
    }
    RESERVATION_NIGHT {
        bigint id PK
        bigint reservation_id FK "NOT NULL, 취소·만료 시 행 삭제"
        bigint room_id FK "NOT NULL"
        date stay_date "room_id와 함께 유니크(점유 락)"
        decimal price "예약 시점 가격 스냅샷(등급 할인 전)"
    }
    PAYMENT {
        bigint id PK
        binary16 public_id UK "외부 식별자(UUID)"
        bigint reservation_id FK "NOT NULL, 유니크(1:1)"
        varchar pg_transaction_number
        decimal amount
        varchar method
        varchar card_company "NULL 가능(간편결제)"
        varchar status
        datetime paid_at
        datetime canceled_at
    }
    PAYMENT_CANCEL_HISTORY {
        bigint id PK
        bigint payment_id FK
        varchar cancel_reason
        decimal cancel_amount
    }
    REVIEW {
        bigint id PK
        bigint member_id FK
        bigint accommodation_id FK
        bigint reservation_id FK "NOT NULL, 유니크(1:1)"
        varchar nick_name
        decimal rating
        varchar content
        datetime created_at
        datetime deleted_at
    }
    OUTBOX_EVENT {
        bigint id PK
        varchar event_id UK "36자, 소비 쪽 event_log의 event_id와 같은 값"
        varchar event_type
        varchar topic
        varchar message_key "파티션 키(숙소 id)"
        varchar payload_type "메시지 클래스 이름"
        text payload "JSON"
        varchar status "PENDING, SENT"
        int attempt_count "전송 시도 횟수"
        datetime sent_at
    }
    EVENT_LOG {
        bigint id PK
        varchar event_id UK "36자"
        varchar event_type
        bigint aggregate_id "FK 없음"
        datetime created_at
    }
```

## 관계 메모

- 점유(`RESERVATION_NIGHT`)는 예약이 차지한 밤마다 한 행이고 `(room_id, stay_date)`가 유니크라서 같은 객실 같은 밤의 이중 예약을 DB가 막는다. 예약이 취소·만료되면 행을 지우고, 확정·이용 완료 예약의 행은 남긴다. 운영자가 막은 날짜는 `ROOM_BLOCKED_DATE`에 따로 둔다(ADR 0013).
- 날짜별 가격은 `ROOM_SPECIAL_PRICE`의 특별 요금 행만 두고, 행이 없는 날은 `ROOM.base_price`다. 합계와 평균은 `Room.calculateStayPrice`가 계산한다(ADR 0013, 0014).
- `EVENT_LOG.aggregate_id`는 FK가 없는 `Long`이다(멱등성 확인에 쓰는 이벤트 기록).
- `PAYMENT`와 `PAYMENT_CANCEL_HISTORY`는 현재 1:1이다.
- `OUTBOX_EVENT`는 반드시 Kafka로 전달되어야 하는 이벤트(지금은 리뷰 등록·삭제)를 본 작업과 같은 트랜잭션에 기록하는 행이다. FK는 없다. 스케줄러가 `PENDING` 행을 Kafka로 보내고 `SENT`로 바꾼다(ADR 0017).
