# 0006. Fixture의 범위와 HTTP 클라이언트

- 상태: 채택 (Fixture 범위와 클라이언트 소유에 관한 결정은 0010이 대체)
- 날짜: 2026-11-01

## 맥락
- 테스트 데이터는 용도별 Fixture로 준비한다(`MemberFixture` 등). 강의의 `TestFixture`는 하나의 큰 클래스다.
- 인증이 필요한 API를 호출하려면 `TestRestTemplate`에 기본 토큰 인터셉터를 달아야 한다. 스프링이 주입하는 `TestRestTemplate`은 컨텍스트에서 하나만 만들어 모든 테스트가 공유한다.

## 결정
- Fixture는 `@Component`와 **프로토타입 범위**(`@Scope("prototype")`)의 빈으로 만든다. 테스트 인스턴스는 메서드마다 새로 만들어지므로 주입되는 Fixture도 테스트마다 새로 생긴다.
- Fixture 안에서 `TestRestTemplate`을 직접 만든다(`LocalHostUriTemplateHandler`로 서버 포트 지정). 공유 빈에 인터셉터를 달지 않는다.
- 테스트 클래스는 `TestRestTemplate`을 주입받지 않고 Fixture의 `client()`로 API를 호출한다. 그래야 Fixture가 로그인하며 심은 기본 토큰이 테스트의 호출에도 적용된다.
- 기본 토큰은 `Authorization` 헤더가 없는 요청에만 붙는 인터셉터로 심는다(강의 방식). 요청에 직접 붙인 헤더가 우선한다.
- Fixture는 `com.pdg.reservation.fixture` 패키지에 두고, 인프라(`support`)와 분리한다.
- 회원처럼 API가 있는 데이터는 API로 만들고, API가 없는 데이터(숙소, 객실, 재고)는 해당 Fixture가 리포지터리로 저장한다.
- 임의 값 생성은 `MemberGenerator`(정적 메서드)에 두고 Fixture와 테스트가 정적 임포트로 쓴다. 가입 테스트마다 값이 겹치지 않도록 이메일, 닉네임 등을 모두 임의로 만든다.
- 잘못된 값 목록은 `TestDataSource`에 값의 종류별로 한곳에 모은다(`invalidEmails()` 등). 여러 도메인이 같은 목록을 중복해서 만드는 것을 막기 위해서다.
- 잘못된 요청은 `MemberGenerator.generateSignupRequestWithNickName(값)`처럼 필드 하나만 바꾸고 나머지는 임의 값으로 채운 요청을 돌려주는 정적 메서드로 만든다.

## 검토한 대안
- 싱글톤 Fixture: 토큰 설정이 다음 테스트로 새서 격리가 깨진다.
- 테스트마다 `new`: 빈 주입이 필요 없지만 방침(주입)과 맞지 않는다.
- 테스트와 Fixture에 프로토타입 `TestRestTemplate`을 각각 주입: 주입받는 자리마다 다른 인스턴스가 들어와서, Fixture가 심은 기본 토큰이 테스트의 호출에 적용되지 않는다. 스프링에는 "테스트 하나당 하나" 범위가 없다.
- 조립 클래스(`TestFixture`)가 클라이언트 하나를 만들어 용도별 Fixture에 넘기는 방식: 클라이언트를 공유할 수 있지만 지금은 API로 만드는 Fixture가 하나뿐이라 과하다.

## 결과
- `@Component` 스캔 방식이라 나중에 슬라이스 테스트(`@WebMvcTest` 등)를 도입하면 Fixture가 필요로 하는 빈이 없어 실패할 수 있다. 그때 `@TestConfiguration`으로 바꾼다.
- API로 만드는 Fixture가 둘 이상이 되면 Fixture마다 클라이언트가 따로 생겨 기본 토큰이 서로 보이지 않는다. 그때 조립 클래스로 클라이언트를 공유하거나 토큰을 인자로 넘기는 방식을 다시 정한다.
- `List.addFirst`는 Java 21 이후 메서드라 이 프로젝트(Java 17)에서는 `add(0, ...)`를 쓴다.
