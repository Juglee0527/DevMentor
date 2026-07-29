# DevMentor 데이터베이스 설계

## 1. 스키마 관리

- 로컬 개발: Hibernate `ddl-auto=update`
- 테스트: Hibernate `ddl-auto=create-drop`
- 테스트 DB: PostgreSQL 17 Testcontainers

현재는 빠르게 검증 가능한 MVP 골격에 맞춘 선택입니다. 운영 배포 전에는 Flyway 또는 Liquibase 기반 명시적 migration으로 전환해야 합니다.

## 2. Entity 관계

```text
User 1 ─ N ChatRoom 1 ─ N ChatMessage
User N ─ M Skill
Skill 1 ─ N Concept
User 1 ─ N UserConceptStatus N ─ 1 Concept
User 1 ─ N Assessment N ─ 1 Concept
ChatMessage 1 ─ N Assessment
User 1 ─ N AiFeedback N ─ 1 ChatMessage
```

## 3. 주요 테이블

| 테이블 | 책임 | 주요 제약 |
| --- | --- | --- |
| `users` | 사용자 프로필 | 닉네임·경력·역할·목표 필수 |
| `user_interested_skills` | 사용자 관심 기술 | 사용자·기술 조합 unique |
| `chat_rooms` | 사용자별 대화방 | 사용자 FK |
| `chat_messages` | 대화 메시지 | 대화방 FK, 생성순 인덱스 |
| `skills` | 상위 기술 | code unique |
| `concepts` | 기술별 개념 | 기술·code 조합 unique |
| `user_concept_statuses` | 사용자별 이해 상태 | 사용자·개념 unique, 점수 0~100 |
| `assessments` | 확인 질문 평가 | 점수 0~100, 사용자·개념·메시지 FK, 사용자·메시지·개념 유일성 |
| `ai_feedback` | AI 답변 검수·학습 동의 snapshot | 사용자·메시지 unique, 동의·삭제 시각 인덱스 |

## 4. enum

- `MessageRole`: `USER`, `ASSISTANT`, `SYSTEM`
- `ConceptDifficulty`: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`
- `LearningStatus`: `NOT_STARTED`, `LEARNING`, `UNDERSTOOD`, `NEEDS_REVIEW`
- `FeedbackRating`: `HELPFUL`, `NOT_HELPFUL`

`chat_messages`의 AI 답변에는 provider, model tag·version, 프롬프트 버전, 응답 시간, fallback 실패 유형, RAG 문서 ID를 저장합니다. `ai_feedback`은 피드백 시점의 질문·생성 답변과 이 메타데이터를 snapshot하여 이후 모델 설정 변경과 분리합니다.

피드백 삭제는 `deleted_at`과 동의 해제로 추적합니다. 철회된 행은 학습 데이터 집계에서 제외하며, 실제 보존 기간과 물리 파기 정책은 인증·개인정보 운영 정책을 정하기 전까지 운영 배포할 수 없습니다.

## 5. 초기 데이터

`SkillDataInitializer`가 코드 기준으로 누락된 기술과 개념만 추가합니다. 초기 기술은 Java, Spring, Spring Boot, JPA, Database, Redis, React, Git, Docker입니다.
