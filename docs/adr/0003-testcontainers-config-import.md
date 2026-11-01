# 0003. 컨테이너 설정을 @Import로 분리

- 상태: 채택
- 날짜: 2026-11-01

## 맥락
- 컨테이너를 띄우는 `TestContainersConfig`를 통합 테스트가 어떻게 가져다 쓸지 정해야 했다.
- `@SpringBootTest`와 `@ActiveProfiles("test")` 설정과 컨테이너를 한 곳에 묶을지 분리할지 논의했다.

## 결정
- `@IntegrationTest`(합성 애노테이션)는 `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, 테스트용 비밀번호 인코더 설정(`TestPasswordEncoderConfig`, ADR 0009)을 담는다. 컨테이너는 담지 않는다. `classes`는 쓰지 않는다(메인 클래스를 자동 탐색).
- `TestContainersConfig`는 `@TestConfiguration`으로 두고, 컨테이너가 필요한 테스트가 `@Import(TestContainersConfig.class)`로 명시한다.

## 검토한 대안
- 상속(`IntegrationTestBase`): 상속 구조가 생기고 컨테이너를 쓰지 않는 테스트에도 따라온다.
- 컨테이너까지 `@IntegrationTest`에 포함: 통합 테스트는 대부분 컨테이너가 필요해서 반복이 줄지만, 컨테이너 없는 선택지가 사라진다.
- `@Configuration`으로 두고 스캔: 모든 `@SpringBootTest`에 컨테이너가 자동으로 붙는다.

## 결과
- 통합 테스트 클래스마다 `@IntegrationTest`와 `@Import(TestContainersConfig.class)`를 같이 쓴다.
- `@Import`를 빠뜨리면 DB 주소가 없어서 연결 실패로 끝난다. 개발 DB로 붙는 사고는 막는다(ADR 0002).
- 반복이 부담이 되면 두 애노테이션을 하나로 묶는 합성 애노테이션을 만든다.
