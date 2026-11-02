# 0010. 공유 ApiClient와 기본 사용자 초기화

- 상태: 채택
- 날짜: 2026-11-02
- 대체: 0006의 "Fixture는 프로토타입 범위", "Fixture 안에서 TestRestTemplate을 직접 만든다", "테스트는 Fixture의 client()로 호출한다" 결정

## 맥락
- 0006은 Fixture마다 `TestRestTemplate`을 만들어 가졌다. 숙소 조회처럼 회원이 필요 없는 공개 API 테스트도 `memberFixture.client()`를 써야 했다.
- 예약부터는 인증이 필요한 Fixture(예약, 결제, 리뷰)가 여럿 생긴다. 0006의 "결과"에 적어 둔 대로 Fixture마다 클라이언트가 따로 있으면 한 Fixture가 심은 기본 토큰이 다른 Fixture에 보이지 않는다.
- 스프링에는 "테스트 메서드 하나당 하나" 범위가 없다. 프로토타입 빈은 주입 지점마다 새 인스턴스가 들어온다.

## 결정
- `support/ApiClient` 싱글톤 `@Component` 하나를 모든 Fixture와 테스트가 주입받아 쓴다. `TestRestTemplate`을 상속하지 않고 안에서 `new`로 만들어 `rest()`로 노출한다.
- 기본 `Authorization`은 `ApiClient`의 필드 하나로 관리하고, 인터셉터는 생성할 때 한 번만 단다. 요청에 직접 붙인 헤더가 있으면 그것이 우선하고, `authenticate`를 다시 부르면 마지막 값이 이긴다.
- 각 테스트 클래스의 `@AfterEach`에서 `apiClient.reset()`으로 기본 사용자를 비운다.
- Fixture는 상태가 없으므로 `@Scope` 없이 싱글톤 `@Component`로 둔다. Fixture가 생성자로 `ApiClient`를 주입받는다.
- `MemberFixture.client()`는 `ApiClient.rest()`를 돌려준다. 기존 호출은 그대로 쓴다.
- `LoginSession.bearer()`로 헤더 값을 만든다.

## 검토한 대안
- Fixture마다 프로토타입 `TestRestTemplate` 주입: 주입 지점마다 다른 인스턴스가 들어와서 토큰이 공유되지 않는다.
- 테스트 메서드 범위(custom scope): 같은 테스트 안에서는 같은 인스턴스를 줄 수 있지만 범위 구현, 비우는 리스너, 등록 설정이 필요해 인프라가 늘어난다.
- 명시적 토큰(인터셉터 없음): 숨은 상태가 없어 가장 단순하고 다중 사용자에 자연스럽다. 대신 보호된 API를 호출할 때마다 헤더를 쓰고 인증이 필요한 Fixture가 `session`을 인자로 받아야 한다. 기본 사용자의 편의를 잃는다.
- `ApiClient`가 `TestRestTemplate`을 상속: 테스트 클래스 변경이 조금 더 작지만 공개 인터페이스가 넓고, 부트가 등록하는 `TestRestTemplate` 빈과 같은 계열 타입이 된다.

## 결과
- 테스트는 순차 실행을 전제로 한다. JUnit 병렬 실행을 켜면 공유 `ApiClient`의 기본 사용자가 서로 덮어써진다. `TRUNCATE` 정리 방식도 병렬과 맞지 않아서, 병렬이 필요해지면 `ApiClient`만이 아니라 격리 전략 전체를 다시 설계한다.
- 새 테스트 클래스는 `@AfterEach`에서 `apiClient.reset()`을 불러야 한다. 빠뜨리면 이전 테스트의 토큰이 샌다. 빼먹는 일이 반복되면 `TestExecutionListener` 하나로 모으는 것을 검토한다.
- 기본 사용자는 한 테스트에 한 명이다. 다른 사용자는 요청에 `Authorization`을 직접 붙인다.
- `ApiClient`는 `LocalHostUriTemplateHandler`를 싱글톤 생성 시점에 설정한다. 이 핸들러가 요청 시점에 서버 포트를 읽는다고 알고 있고, 전체 테스트가 통과하는 것으로 확인했다.
