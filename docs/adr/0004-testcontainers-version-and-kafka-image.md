# 0004. Testcontainers 버전과 Kafka 이미지

- 상태: 임시
- 날짜: 2026-11-01

## 맥락
- Docker Desktop 4.52(Engine 29.0.1)는 최소 API 버전이 1.44다. Spring Boot 3.5.6이 관리하는 Testcontainers 1.21.3은 더 낮은 API 버전으로 요청해서 `BadRequestException (Status 400)`이 나고 컨테이너를 띄울 수 없었다.
- Kafka `apache/kafka:3.9.0`은 Testcontainers에서 `advertised.listeners cannot use the nonroutable meta-address 0.0.0.0`으로 종료됐다. 원인은 확인하지 못했다. 같은 이미지를 `docker run`으로 단독 실행하면 정상이었다.

## 결정
- `build.gradle`에서 `ext['testcontainers.version'] = '1.21.4'`로 Spring Boot 관리 버전을 덮어쓴다.
- Kafka 이미지는 `apache/kafka:3.8.0`을 쓴다.

## 검토한 대안
- 사용자 홈의 `.docker-java.properties`에 `api.version=1.44`: 이 PC에만 적용되어 다른 PC와 CI에서 각자 설정해야 한다.
- Docker 다운그레이드: 환경을 거꾸로 돌리는 방법이라 채택하지 않았다.
- Testcontainers 2.x: Spring Boot 3.5와의 호환을 확인하지 못했다.

## 결과
- 덮어쓰기가 있어서 Spring Boot를 올릴 때 관리 버전과 어긋날 수 있다.
- 다시 볼 조건
  - Spring Boot를 올려 관리 버전이 1.21.4 이상이 되면 `ext` 덮어쓰기를 지운다.
  - Kafka 3.9.0 실패 원인을 조사한 뒤 이미지를 올릴지 판단한다.
