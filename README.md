# 🏨 예약의 정석 (Standard of Reservation)
![진행도](https://img.shields.io/badge/Status-In_Progress-yellow)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-Redisson-red)

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

### 🔥 Issue 2: 다중 서버 환경에서의 초과 예약(따닥) 방지 로직 구현
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

## 🎯 4. 개발 진행 상황 (To-Do Roadmap)

- [x] 도메인 엔티티 및 DB 설계 (ERD 클라우드 연동)
- [x] Spring Security + JWT + Redis 블랙리스트 기반 인증/인가 구현
- [x] 숙소 및 객실 도메인 조회(Read) API 기본 구현
- [x] 객실 및 일자별 재고(Inventory) 관리 도메인 로직 구현
- [x] Redisson + 낙관적 락을 활용한 안전한 예약 생성 API 구현
- [x] 미결제 예약 롤백 및 Spring Event(`@TransactionalEventListener`)를 활용한 결합도 분리
- [ ] 조회 화면(숙소/객실) 쿼리 최적화 (N+1 문제 해결 및 페이징 처리)
- [ ] 대용량 트래픽 대비 메인 데이터베이스 인덱싱(Indexing) 적용 계획
- [ ] 시스템 확장을 고려한 Apache Kafka 기반의 비동기 알림 시스템 도입 예정

## ⚙️ 5. 기술 스택 (Tech Stack)
- **Backend:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, QueryDSL
- **Database:** MySQL 8.0, Redis (Redisson, Lettuce)
- **Architecture:** Layered Architecture, Event-Driven Architecture (Spring Events)
