# 개발 가이드

프로젝트 구동, 테스트, 문서 주소와 clone 후 설정을 모아 둔다. 아직 정해지지 않은 항목은 `미작성`으로 표시했다.

## clone 후 설정

저장소를 새로 clone한 뒤 한 번씩 실행한다. Git 설정은 저장소에 같이 올라가지 않아서 clone마다 필요하다.

```bash
git config core.hooksPath .githooks
```

- Git Hook(`commit-msg`, `pre-commit`)을 켠다. 커밋 메시지 형식, `.env` 커밋, `main` 직접 커밋을 검사한다.
- macOS·Linux에서는 훅 파일에 실행 권한이 필요하다: `chmod +x .githooks/*`

```bash
git config commit.template gitmessage.txt
```

- 커밋할 때 편집창(`git commit`을 `-m` 없이 실행)에 커밋 규칙 안내가 주석으로 보인다.

확인:

```bash
git config core.hooksPath
git config commit.template
```

각각 `.githooks`, `gitmessage.txt`가 출력되면 등록된 것이다.

규칙 본문은 [git-rules.md](git-rules.md)에 있다.

## 프로젝트 구동

미작성

## 테스트

미작성

## 문서·API 주소

미작성 (Swagger 설정 후 추가)
