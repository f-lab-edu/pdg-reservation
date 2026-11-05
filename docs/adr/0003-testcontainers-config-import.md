# 0003. 통합 테스트 설정을 @IntegrationTest에 모은다

- 상태: 채택 (2026-11-05 변경)
- 날짜: 2026-11-01

## 맥락
- 컨테이너를 띄우는 `TestContainersConfig`를 통합 테스트가 어떻게 가져다 쓸지 정해야 했다.
- `@SpringBootTest`와 `@ActiveProfiles("test")` 설정과 컨테이너를 한 곳에 묶을지 분리할지 논의했다.

## 결정
- `@IntegrationTest`(합성 애노테이션)는 `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, 운영 빈을 바꿔 끼우는 테스트 빈 설정(`TestBeanConfig`: 비밀번호 인코더(ADR 0009), PG 대역)과 컨테이너 설정(`TestContainersConfig`)을 모두 담는다. `classes`는 쓰지 않는다(메인 클래스를 자동 탐색).
- 두 설정 클래스는 `@TestConfiguration`으로 두고, 통합 테스트는 `@IntegrationTest` 하나만 붙인다.
- 컨테이너 설정은 `TestBeanConfig`와 합치지 않는다. 클래스 로딩 시점의 `static` 블록으로 컨테이너를 띄우는 인프라이고, 시점이 스프링 빈 생성과 얽혀 있다(ADR 0002).

## 검토한 대안
- 상속(`IntegrationTestBase`): 상속 구조가 생기고 컨테이너를 쓰지 않는 테스트에도 따라온다.
- `@Configuration`으로 두고 스캔: 모든 `@SpringBootTest`에 컨테이너가 자동으로 붙는다.
- 컨테이너는 클래스마다 `@Import`로 명시(처음의 결정): 컨테이너 없는 통합 테스트를 남길 수 있지만 지금은 모든 통합 테스트가 컨테이너를 쓰고, 클래스 15곳에 같은 줄이 반복되었다.

## 결과
- 통합 테스트 클래스는 `@IntegrationTest`만 붙인다. `@Import`를 빠뜨려 DB 주소가 없어서 연결 실패로 끝나는 일(ADR 0002)이 생기지 않는다.
- 스프링 컨텍스트 수는 변하지 않는다. 컨텍스트를 가르는 것은 설정 클래스의 목록이 같은지 여부인데, 이미 모든 통합 테스트가 같은 설정을 써 왔다. 스파이(`@MockitoSpyBean`)를 쓰는 클래스만 별도 컨텍스트를 만든다.
- 컨테이너 없이 도는 통합 테스트가 필요해지면 그 테스트는 `@IntegrationTest`를 쓰지 않고 따로 설정한다. 엔티티·도메인 단위 테스트는 스프링을 띄우지 않으므로 해당하지 않는다.
- 테스트 빈 설정이 한 곳에 모여 "테스트에서 운영과 달라지는 것"을 한눈에 볼 수 있다.
