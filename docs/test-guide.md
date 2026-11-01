# 테스트 가이드

이 프로젝트의 테스트 방식과 규칙. 참고 강의: Spring Boot TDD - 입문부터 실전까지 정확하게.
`미정`, `미작성`으로 표시한 항목은 아직 정하지 않았거나 코드가 없어서 쓰지 못한 부분이다.

## 1. 방향

- **API 통합 테스트 중심**으로 간다. 실제 서버를 띄우고 HTTP 요청과 응답으로 요구사항을 검증한다.
- 단위 테스트, 서비스 테스트, 리포지터리 테스트로 층을 나눠 피라미드를 쌓지 않는다. 구분보다 "요구사항 검증이 자동화되었는가"를 중요하게 본다.
- **Mock 같은 테스트 대역은 최소로 쓴다.** 남용하면 설계 변경 비용이 늘고 운영 코드는 문제인데 테스트만 통과하는 거짓 음성이 늘어난다. 실제 구성요소를 구동하거나 제어할 수 없을 때(예: 외부 PG)에만 대역(Fake)을 고려한다.
- **복잡하거나 필요한 곳에는 단위 테스트를 추가한다.** 예: 엔티티 생성·상태 전이처럼 API에서 직접 만들기 어려운 로직
- DTO는 따로 테스트하지 않고 API 테스트로 검증한다.
- 테스트만을 위해 인터페이스를 만들거나 클래스를 잘게 쪼개거나 구조를 바꾸지 않는다.

## 2. 환경

- DB(MySQL), Redis, Kafka는 Testcontainers로 실제 컨테이너를 띄운다. 컨테이너는 테스트 전체가 한 번만 띄워서 공유하고, 테스트마다 새로 띄우지 않는다.
- 테스트는 `test` 프로필로 돌린다. 개발용 샘플 데이터(`data.sql`)는 테스트에서 실행하지 않는다(`spring.sql.init.mode: never`).
- API 호출은 `TestRestTemplate`을 쓴다.
- 테스트 실행에는 Docker가 실행 중이어야 한다.

```bash
./gradlew test
```

## 3. 데이터

- 테스트가 쓰는 데이터는 **용도별 Fixture로 준비**한다. `TestFixture` 하나로 고정하지 않고 용도에 맞게 `xxxFixture`로 나눈다. 예: 회원 준비용 `MemberFixture`의 `createMember()`
- 회원처럼 API가 있는 데이터는 API를 호출해서 만든다. 스키마가 바뀌어도 테스트가 남는다.
- API가 없는 데이터(숙소, 객실, 재고)는 해당 Fixture가 리포지터리로 직접 저장한다. 이런 코드는 스키마 변경 때 고칠 곳을 줄이기 위해 Fixture 안에만 둔다.
- **데이터 격리는 cleanUp으로 한다.** `@AfterEach`에서 테스트가 사용하는 저장소만 초기화한다. DB는 `DatabaseCleanUp.truncateAllTables()`로 외래 키 검사를 끄고 테이블을 비운 뒤 다시 켠다. Redis는 아래 규칙대로 `RedisCleanUp`을 쓴다. 로그인처럼 Redis를 건드리는 테스트에서 추가한다.
- Fixture는 프로토타입 범위의 빈으로 두고, 안에서 `TestRestTemplate`을 직접 만들어 쓴다. 공유되는 `TestRestTemplate`에 기본 토큰 인터셉터를 달면 테스트 사이에 새기 때문이다. 토큰이 필요한 API를 테스트할 때 적용한다.
- **임의 테스트 데이터**를 생성해서 쓴다(이메일, 비밀번호 등). 고정 값을 쓰면 테스트끼리 값이 겹칠 수 있다.
- 리소스 ID는 하드코딩하지 않고 응답에서 꺼내 재사용한다.
- 검증은 기본적으로 API 응답으로 한다. 응답에 나오지 않는 결과(예: 암호화되어 저장된 비밀번호, 아웃박스 기록)만 DB를 직접 조회해서 확인한다.
- Redis는 `RedisCleanUp.flushAll()`로 전체 삭제한다. 블룸 필터의 설정도 같이 지워지므로, 비운 직후 `AccommodationBloomFilter.init()`으로 빈 필터를 다시 만든다.
- 숙소를 만드는 `AccommodationFixture`는 저장한 뒤 블룸 필터에 `add(id)`를 한다. 필터가 비어 있으면 DB에 있는 숙소도 `ACC_NOT_FOUND`가 되기 때문이다.
- Kafka는 따로 비우지 않는다. 토픽을 지우면 재시도 토픽과 컨슈머 그룹이 불안정해지기 때문이다. 대신 비동기 처리가 끝나는 흐름은 Awaitility로 결과를 기다린 뒤 테스트를 끝낸다.

## 4. TDD 절차

1. 다루고자 하는 테스트 시나리오의 목록을 작성한다.
2. 목록에서 정확히 한 가지 항목을 실제 실행 가능한 구체적인 테스트로 전환한다.
3. 전환된 테스트(및 모든 이전 테스트)를 통과하도록 코드를 변경한다.
4. 선택적으로 리팩터링해서 구현 설계를 개선한다.
5. 목록이 비워졌는지 확인한다. 발견되는 대로 목록에 항목을 추가한다.

## 5. 구조

```
src/test/java/com/pdg/reservation/
├── api/v1/members/POST_specs.java        # POST /api/v1/members
├── api/v1/members/login/POST_specs.java  # POST /api/v1/members/login
├── member/entity/MemberTest.java         # 엔티티 단위 테스트(로직이 생기면)
├── support/                              # IntegrationTest, TestContainersConfig, DatabaseCleanUp, RedisCleanUp
└── fixture/                              # xxxFixture, MemberGenerator(임의 값), TestDataSource(잘못된 값 목록)
```

- API 테스트는 URL을 패키지로 따라가고(버전 포함), HTTP 메서드별로 `GET_specs`, `POST_specs` 클래스를 둔다.
- 단위 테스트는 운영 코드 패키지를 그대로 따라가며 `XxxTest`로 이름 짓는다.
- 패키지 이름: 고정 경로는 그대로, 경로 변수는 변수 이름만 쓴다(`/accommodations/{id}` → `accommodations/id`). 하이픈은 제거한다.
- 인프라(`support`)와 데이터 준비(`fixture`)는 분리한다.
- 테스트 메서드 이름은 한글 문장으로 쓰고, 띄어쓰기 대신 `_`를 쓴다. `@DisplayName`은 쓰지 않는다. 메서드 이름 자체가 요구사항이 되도록 "조건_결과" 형태로 쓴다.
  예: `올바른_정보로_가입하면_201을_반환한다`, `이메일이_중복이면_409를_반환한다`
- 같은 필드의 규칙 테스트(유효하지 않은 값이면 400, 한계 값이면 성공)는 같은 클래스에 나란히 둔다. `@Nested`는 쓰지 않는다.
- 길이나 범위 제한이 있는 값은 **2값 경계값 분석**으로 시험한다. 허용 범위의 최소·최대 값(성공)과 그 바로 바깥 값(거절)을 모두 확인한다. 예: 비밀번호 8~20자는 7자 거절, 8자 성공, 20자 성공, 21자 거절. 평범한 값만 시험하면 한 칸 어긋난 한계(예: 최대 19자)를 놓친다.
  - 입력 검증(`@Size`, `@Pattern`)은 검증 프레임워크가 길이 비교를 구현하므로 2값으로 충분하다. 직접 구현하는 계산·분기 로직(요금, 재고, 한도 등)은 경계 안쪽 값까지 포함한 3값 방식을 고려한다.
  - 유효하지 않은 값은 종류(`null`, 빈 문자열, 공백, 형식 오류, 구성 규칙 위반, 길이 위반)별로 대표 값을 하나씩 두는 동치 분할로 고른다. 같은 종류의 값을 여러 개 늘리지 않는다.
  - 잘못된 값 쪽 표시 이름은 `최소 길이 미만 (7자)`, `최대 길이 초과 (21자)`처럼 경계 이름과 숫자를 함께 쓴다. 성공 쪽 테스트 이름은 `비밀번호가_최소_길이_8자이면_가입된다`처럼 쓴다.
  - 성공 경계 테스트는 구현 전에도 통과하는 회귀 방지용이라, 한계를 일부러 한 칸 어긋나게 만들어 실패하는지 확인한다.
- 잘못된 값 목록은 `fixture/TestDataSource`에 값의 종류별로 한곳에 모은다(`invalidEmails()` 등). 새 목록을 만들기 전에 이 파일을 먼저 확인한다. 도메인마다 정책이 다른 값은 메서드 이름에 도메인을 붙인다.
- 잘못된 요청은 `MemberFixture.signupRequestBuilder()`가 돌려주는 유효한 값으로 채운 빌더에서 한 필드만 덮어써서 만든다. 운영 DTO에 `toBuilder`를 추가하지 않는다.
- 테스트 본문은 AAA 패턴으로 작성하고 `// Arrange`, `// Act`, `// Assert` 주석으로 구분한다.
  - Arrange: 테스트에 필요한 데이터와 상태를 준비한다.
  - Act: 검증할 동작을 한 번 실행한다.
  - Assert: 결과를 검증한다.
  - Act에는 이 테스트가 검증하려는 동작을 드러낸다. 전제 조건은 Arrange에 두고 Fixture로 묶어도 된다. 같은 동작(예: 로그인)이 어떤 테스트에서는 Act, 다른 테스트에서는 Arrange일 수 있다.
- 검증은 AssertJ(`assertThat`)로 통일한다. JUnit의 `Assertions`(`assertEquals`, `assertAll` 등)는 쓰지 않고, JUnit은 `@Test`, `@AfterEach` 같은 구조용으로만 쓴다.
  - 같은 이름의 `Assertions` 클래스가 AssertJ(`org.assertj.core.api`)와 JUnit(`org.junit.jupiter.api`)에 둘 다 있어서, 섞어 쓰면 import를 보지 않고는 어느 쪽인지 알 수 없고 와일드카드 import에서 같은 이름 메서드가 충돌할 수 있다.
  - 인자 순서가 반대다. JUnit의 `assertEquals(expected, actual)`은 기대값이 앞이고, AssertJ의 `assertThat(actual).isEqualTo(expected)`는 실제값이 앞이다. 섞으면 순서를 실수해서 실패 메시지가 거꾸로 보인다.
  - 실패 메시지 형식이 다르다. AssertJ는 `Expecting actual: ... to be equal to: ...`, JUnit은 `expected: <...> but was: <...>`로 나온다. 하나로 통일해야 실패 원인을 읽는 방식이 일정하다.
## 6. 미정

- Kafka 소비가 늦어져서 다음 테스트와 겹치는 일이 생기면 컨슈머 랙 대기 도우미를 추가할지
- 로그인이 반복되는 테스트가 늘 때 `PasswordEncoder`를 테스트용으로 교체할지(속도 개선)
