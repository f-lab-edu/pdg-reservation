# 🏨 예약의 정석 (Standard of Reservation)
![진행도](https://img.shields.io/badge/Status-In_Progress-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-Redisson-red)
![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-007ACC?style=flat-square&logo=jquery&logoColor=white)

> **안전하고 신뢰할 수 있는 B2C 숙박 예약 시스템 API** > 대용량 트래픽이 몰리는 상황에서도 '초과 예약(Overbooking)'이 발생하지 않도록 견고한 동시성 제어와 데이터 정합성 보장에 초점을 맞추어 개발 중인 프로젝트입니다.

## 📌 1. 프로젝트 목표 및 개요
- 안전한 인증/인가 로직과 읽기(Read) 성능 최적화를 통해 실제 서비스 운영 수준의 백엔드 시스템을 구축합니다.
- 객체지향 설계(OOP) 원칙과 클린 코드를 지향하며, 유지보수가 용이한 아키텍처를 구성하고 있습니다.

## 📊 2. 데이터베이스 설계 (ERD)
> 🔗 **[ERD 클라우드에서 전체 테이블 및 연관관계 명세 자세히 보기](https://www.erdcloud.com/d/BGuoJkprAskGBsHAu)**

## 🚀 3. 핵심 기술적 의사결정 (Troubleshooting)

### 🔥 Issue 1: Spring Security + JWT + Redis 기반의 안전한 인증/인가 및 블랙리스트(Blacklist) 처리
Stateless한 JWT 인증 방식의 한계(토큰 강제 만료 불가)를 극복하기 위해 Redis를 활용한 블랙리스트 아키텍처를 도입했습니다.
- **보안성 강화:** 사용자가 로그아웃을 요청하면, 해당 Access Token의 남은 유효시간만큼 Redis에 블랙리스트로 등록하여 악의적인 토큰 탈취 및 재사용을 완벽하게 차단합니다.
- **빠른 검증:** 커스텀 `JwtAuthenticationFilter`를 구현하여, 매 요청 시 Redis 블랙리스트 여부를 인메모리 기반 O(1)의 속도로 빠르게 검증합니다.

### 🔥 Issue 2: 다중 서버 환경에서의 초과 예약 방지 로직 구현
이벤트 기간 등 사용자가 동시에 예약을 시도할 때 발생할 수 있는 데이터 정합성 문제를 3중 방어선으로 제어했습니다.
- **1차 방어 (트래픽 제어):** `Redisson` 기반의 분산 락(Distributed Lock)을 적용하여 DB 부하를 줄이고 메모리 단에서 요청을 직렬화했습니다.
- **2차 방어 (비즈니스 검증):** `ReservationValidator` 객체를 분리하여 순수 자바 코드로 비즈니스 규칙을 빠르게 검증합니다.
- **3차 방어 (데이터 무결성):** `RoomInventory` 엔티티에 JPA 낙관적 락(`@Version`)과 DB 복합 유니크 키를 적용하여, 동시 업데이트로 인한 초과 예약을 원천 차단했습니다.

### 🔥 Issue 3: 미결제 예약 만료 시 안전한 롤백 및 재고 복구 (EDA 도입)
고객이 예약을 선점한 후 결제하지 않았을 때, 트랜잭션 생명주기를 안전하게 통제하여 재고를 복구하는 로직을 설계했습니다.
- **이벤트 주도 아키텍처 (EDA) 적용:** 트랜잭션 롤백 시 발생할 수 있는 '거짓 성공 로그' 방지를 위해 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`를 도입했습니다.
- **의존성 분리:** 예약 서비스는 DB 상태 변경에만 집중하고, 트랜잭션 커밋이 100% 완료된 직후에만 후속 작업(비동기 로깅, 외부 API 호출 등)을 처리하도록 결합도를 낮췄습니다.

### 💡 Issue 4: 숙소 및 객실 도메인 조회(Read) API 설계
숙소 리스트, 상세 정보, 객실 리스트 등 연관 데이터가 많은 '조회(Read)' 중심의 API를 구현했습니다. 
- 복잡한 도메인 관계(숙소-객실-이미지 등)에서 발생할 수 있는 N+1 문제를 방지하기 위해 쿼리 최적화를 고려하여 설계했습니다.

### 🔥 Issue 5: Mock PG 연동을 통한 결제 승인 프로세스 및 장애 대응 설계
실제 결제 수단 연동 전 단계로서 MockPgClient를 구축하고, 외부 API 통신과 내부 DB 트랜잭션 간의 생명주기를 분리하여 시스템 가용성을 확보했습니다.
- 리소스 점유 최소화: PG 승인 요청(Mocking)을 DB 트랜잭션 외부에서 처리하여, 가상의 네트워크 지연(Latency)이 내부 DB 커넥션 풀 고갈(Connection Pool Exhaustion)로 전파되는 현상을 방지했습니다.
- 보상 트랜잭션(Compensation) 설계: DB 영속화 단계에서 예외 발생 시, 이미 승인된 PG 결제를 즉시 강제 취소하는 보상 로직을 구현하여 '결제 완료-예약 누락'이라는 데이터 부정합 상태를 0%를 지향했습니다.
- 예외 섀도잉(Exception Shadowing) 방지: 보상 트랜잭션 실행 중 발생하는 2차 예외를 별도 격리하여, 장애의 근본 원인(Root Cause)이 되는 최초 예외 로그가 유실되지 않도록 방어적 프로그래밍을 적용했습니다.
- 
### 🔥 Issue 6: 분산 락 및 멱등성을 활용한 중복 결제/환불 방어
돈과 직결된 결제 및 취소 도메인에서 발생할 수 있는 '따닥(Double Click)' 요청 및 네트워크 재시도에 따른 중복 처리를 차단했습니다.
- 중복 환불(Double Refund) 방지: 결제 취소 API 진입점에 Redisson 분산 락을 적용하고, 내부에서 결제 상태(Status)를 재검증하는 2중 방어벽을 구축하여 동일 건에 대한 중복 환불 사고를 원천 차단했습니다.
- 데이터 정합성 보장: OneToOne 관계의 결제-예약 엔티티에 DB 레벨의 유니크 제약 조건(Unique Constraint)을 추가하여, 로직 오류로 인한 중복 레코드 생성을 물리적으로 방어했습니다.
- 조회 최적화: 예약 취소 시 연관된 결제 데이터를 함께 조회할 때 발생할 수 있는 N+1 문제를 방지하기 위해 Fetch Join을 적용하여 쿼리 1번으로 데이터 정합성 검증과 비즈니스 로직을 완벽히 처리했습니다.
- 
### 🔥 Issue 7: 비동기 이벤트 및 지능형 캐싱을 활용한 고성능 리뷰 시스템
리뷰 등록/삭제 시 발생하는 부수적인 로직과 대량의 조회 트래픽을 효율적으로 처리하기 위해 EDA(이벤트 주도 아키텍처)와 고도화된 캐싱 전략을 도입했습니다.
- 비동기 평점 업데이트 (EDA): @TransactionalEventListener와 @Async를 조합하여 리뷰 작성과 평점 집계 로직을 분리했습니다. 이를 통해 사용자 응답 속도를 개선함과 동시에, 메인 트랜잭션과 후속 로직의 생명주기를 분리하여 시스템 확장성을 확보했습니다.
- 평점 증분 업데이트 O(1): 매번 전체 데이터를 전수 조사 SELECT AVG()하는 대신, 기존 합계와 개수에 증분값만 반영하는 로직을 구현하여 데이터 규모에 관계없이 일정한 집계 성능을 확보했습니다.
- 버전 기반 캐시 및 조회 최적화: 복합 키 캐시 관리를 위해 버전 번호를 도입했습니다. 트래픽이 집중되는 0페이지만 선택적으로 캐싱하고, 이후 페이지는 Redis 조회를 생략하는 설계를 통해 Redis 네트워크 I/O 비용을 획기적으로 절감했습니다.
- 캐시 스탬피드 방어 및 정합성 제어: Redisson 분산 락에 **@Order(1)**을 부여하여 트랜잭션 커밋 완료 후 락이 해제되도록 정밀하게 제어했습니다. 락 획득 후 캐시를 재확인하는 Double-Check 패턴을 적용해 중복 DB 조회를 원천 차단했습니다.
- 캐시 관통(Penetration) 방어 - Bloom Filter: 존재하지 않는 숙소 ID나 리뷰 ID를 반복 조회하여 DB 부하를 일으키는 공격을 방어합니다. Redis Bloom Filter를 프론트엔드 접점에 배치하여, 실제 데이터가 없는 요청은 DB에 도달하기 전에 99% 이상 차단합니다.
캐시 아발란체(Avalanche) 방어 - TTL Jitter: 대규모 캐시가 동일한 시점에 만료되어 DB로 부하가 몰리는 현상을 방지합니다. 각 캐시의 만료 시간(TTL)에 5~10%의 무작위 지터(Jitter) 시간을 추가하여, 캐시 만료 시점을 통계적으로 분산시켰습니다.
  
## 🎯 4. 개발 진행 상황 (To-Do Roadmap)

- [x] 도메인 엔티티 및 DB 설계 (ERD 클라우드)
- [x] Spring Security + JWT + Redis 블랙리스트 기반 인증/인가 구현
- [x] 숙소 및 객실 도메인 조회(Read) API 기본 구현
- [x] 객실 및 일자별 재고(Inventory) 관리 도메인 로직 구현
- [x] Redisson + 낙관적 락을 활용한 안전한 예약 생성 API 구현
- [x] 미결제 예약 롤백 및 Spring Event(`@TransactionalEventListener`)를 활용한 결합도 분리
- [x] 가상의 PG 연동 및 트랜잭션 경계 분리를 통한 고가용성 결제 승인 API 구현
- [x] 증분 업데이트O(1) 및 비동기 이벤트를 활용한 고성능 리뷰 시스템 구축
- [x] 버전 기반 캐싱 및 0페이지 바이패스 전략을 통한 리뷰 조회 성능 고도화
- [x] Redisson + @Order(1): 트랜잭션 정합성 보호 및 캐시 스탬피드 방어 구현
- [x] TTL Jitter: 만료 시간 분산을 통한 캐시 아발란체 방어 적용
- [x] Bloom Filter: 유효하지 않은 요청 차단을 통한 캐시 관통 방어 구현
- [x] 이벤트 로그 테이블을 활용한 비동기 로직의 멱등성 및 정합성 보장
- [ ] 시스템 확장을 고려한 Apache Kafka 기반의 비동기 알림 및 통계 시스템 도입 예정

## ⚙️ 5. 기술 스택 (Tech Stack)
- **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, QueryDSL
- **Database:** MySQL 8.0, Redis (Redisson, Lettuce)
- **Architecture:** Layered Architecture, Event-Driven Architecture (Spring Events)
