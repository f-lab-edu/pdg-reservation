# 0008. 테스트에서 운영 DTO 사용

- 상태: 채택
- 날짜: 2026-11-01

## 맥락
- API 테스트에서 요청을 만들고 응답을 읽는 방법이 필요하다.
- `SignupRequest`, `LoginRequest`는 생성자가 `protected`이고 빌더가 없어서 테스트에서 만들 수 없었다. `LoginResponse`는 기본 생성자가 없어서 JSON을 객체로 읽을 수 없었다.

## 결정
- 테스트에서 운영 DTO(`SignupRequest`, `LoginRequest`, `LoginResponse`)를 그대로 쓴다.
- 요청 DTO에는 `@Builder`와 `@AllArgsConstructor(access = PRIVATE)`를 붙인다. 같은 타입 `String`이 여러 개라 생성자보다 빌더가 읽기 쉽다. Jackson용 `@NoArgsConstructor(access = PROTECTED)`는 유지한다.
- 응답 DTO는 JSON을 읽을 수 있도록 `@NoArgsConstructor(PROTECTED)`와 `@AllArgsConstructor(PRIVATE)`를 붙인다.
- 요청은 `RequestEntity`(`post(...).body(...)`)와 `exchange`로 보내고, 제네릭 응답은 `ParameterizedTypeReference`로 받는다.

## 검토한 대안
- `Map`으로 JSON을 직접 구성: 운영 DTO에 의존하지 않아 API 계약(JSON 키) 변경을 테스트가 잡는다. 키 오타를 컴파일러가 못 잡고 반복이 늘어서 쓰지 않았다.
- 테스트 전용 요청 타입(`record`): 계약은 독립적이지만 타입이 중복된다.
- `@Jacksonized`로 응답 역직렬화: 한 줄로 되지만 기존 DTO 스타일(`@NoArgsConstructor` + `@AllArgsConstructor`)과 맞추기 위해 쓰지 않았다.

## 결과
- 운영 DTO의 필드 이름을 바꿔도 테스트가 같이 바뀌어 통과한다. 외부 클라이언트와의 JSON 계약이 깨져도 테스트가 알려 주지 않는다. API를 소비하는 외부 클라이언트가 생기면 계약 테스트나 REST Docs를 추가한다.
- DTO를 `record`로 바꾸는 일은 R3-1(MapStruct 도입)에서 일괄로 검토한다. 그때 이 결정도 다시 본다.
