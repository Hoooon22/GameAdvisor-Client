# GameAdvisor-Client Cursor 룰

## 1. 커밋 메시지 규칙
- 반드시 아래 커밋 메시지 작성 규칙을 따른다.
- 영어 대신 한국어 사용
- 제목은 50자 이내, 마침표 없이, 현재 시제로 작성
- 본문에는 변경 이유와 내용을 구체적으로 작성
- 여러 변경 사항은 `-`로 구분

### 커밋 메시지 예시
```
feat: 로그인 화면 키보드 UX 개선
- TextInput ref를 사용하여 자동 포커스 기능 추가
- returnKeyType 설정으로 키보드 엔터키 동작 개선
- 전화번호 입력 후 자동으로 비밀번호 입력창으로 포커스 이동
- 비밀번호 입력 후 엔터키로 로그인 가능하도록 개선
```

## 2. 브랜치 전략
- 기능 개발: `feature/기능명`
- 버그 수정: `fix/버그명`
- 리팩토링: `refactor/모듈명`
- 배포: `release/버전명`
- hotfix: `hotfix/이슈명`
- 브랜치 이름은 영어 소문자, 단어는 `-`로 구분

## 3. 코드 스타일
- Java: Google Java Style Guide 준수
- 클래스/메서드/변수명은 의미를 명확히
- 불필요한 주석, 사용하지 않는 import 금지
- 한글 주석 허용(가독성 위주)
- 들여쓰기 4칸, 탭 대신 스페이스
- 파일 끝에 빈 줄 추가

## 4. PR(Pull Request) 규칙
- PR 제목: `[타입] 간단 설명` (예: `[feat] 게임 리스트 UI 추가`)
- PR 본문:
  - 변경 내용 요약
  - 테스트 방법
  - 관련 이슈(있다면)
- 리뷰어 지정 필수
- 본인 PR은 직접 머지 금지(최소 1명 이상 승인 필요)

## 5. 커서 사용 규칙
- 파일/코드 변경 시, 변경 목적과 이유를 명확히 작성
- 자동 생성/포맷팅된 코드도 반드시 리뷰
- 대용량 파일은 chunk 단위로 나눠서 작업
- 파일/폴더 경로는 항상 절대경로 사용
- PowerShell 명령어는 `;`로 구분(`&&` 대신)

## 6. 기타
- 날짜/시간 정보는 인터넷 기준 사용
- AI 자기소개서 등 자동화된 문서 작성 시, 반드시 개인화 및 퇴고
- 모든 커뮤니케이션은 한국어로 진행 