# GameAdvisor Client

JavaFX 기반의 게임 오버레이 클라이언트 및 AI 어드바이저 캐릭터

---

**백엔드 서버는 별도의 GitHub private 레포지토리에서 관리되고 있습니다.**
- 클라이언트(이 저장소)는 Windows 환경에서 개발 및 실행합니다.
- 백엔드는 macOS 환경에서 별도 운영하며, 접근이 필요한 경우 별도 안내를 받으시기 바랍니다.

---

## 주요 특징

- 완전 투명 오버레이로 게임 위에 캐릭터 표시
- 마우스 드래그/투사, 물리 효과, 말풍선, 클릭 회피 등 인터랙티브 기능
- AI 화면 분석 버튼, 실시간 게임 감지 및 맞춤 조언
- Windows API 연동(JNA), JavaFX 기반 UI

## 폴더 구조

```
client/
├── src/
│   └── main/java/com/gameadvisor/client/
│       ├── ui/
│       │   ├── GameAdvisorClient.java
│       │   └── components/
│       │       ├── AdvisorCharacter.java
│       │       ├── SpeechBubble.java
│       │       └── CharacterOverlay.java
│       ├── model/
│       ├── network/
│       ├── service/
│       └── util/
├── build.gradle
└── README.md
```

## 기술 스택

- Java 17
- JavaFX 17
- Gradle
- JNA (Windows API 연동)

## 실행 방법

1. 클라이언트 실행
   ```bash
   cd client
   ./gradlew run
   ```

2. 게임 실행 시 자동으로 캐릭터 오버레이 표시

## 주요 기능

- 마우스 드래그/투사, 물리 효과, 충돌 애니메이션
- 다양한 스타일의 말풍선, 최소화/복원, X버튼 제어
- 클릭 회피 시스템(사용자 클릭 위치 감지 및 회피)
- AI 화면 분석 버튼(게임 상황 분석 및 조언)
- 실시간 게임 감지 및 맞춤 조언

## 커밋 메시지 규칙

```
<type>: <subject>

<body>
```
- type: feat, fix, docs, style, refactor, test, chore
- subject: 50자 이내, 마침표 없이, 현재 시제
- body: 무엇을, 왜 변경했는지 상세히
``` 
