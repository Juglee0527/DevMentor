# DevMentor MVP 검증 기록

## 1. 검증 범위

검증일: `2026-07-28`

이 문서는 2~7단계에서 구현한 기능을 8단계에서 다시 검증한 결과와 현재 한계를 기록합니다. 실제 OpenAI API는 자동·수동 검증에서 호출하지 않고 기본 `AI_CLIENT_MODE=fake`를 사용했습니다.

## 2. 자동 검증

저장소 루트 기준 실행 명령:

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps

cd backend
.\gradlew.bat test --rerun-tasks

cd ..\frontend
npm run build
npm run lint
```

결과:

| 검증 | 결과 |
| --- | --- |
| Docker Compose 설정 | 성공 |
| PostgreSQL 17 health check | `healthy` |
| 백엔드 전체 테스트 | 성공, 22개 |
| PostgreSQL Testcontainers | 사용자·대화·학습·평가 저장/조회 성공 |
| 프론트엔드 TypeScript·프로덕션 빌드 | 성공 |
| ESLint | 성공 |
| Git whitespace 검사 | 성공 |

백엔드 테스트는 다음 경계를 포함합니다.

- 정상·검증 실패·미존재·내부 오류 공통 응답
- Entity 제약, 초기 기술 9개·개념 19개, Repository 정렬·소유권
- 사용자·대화방·USER/ASSISTANT 메시지 PostgreSQL 저장
- AI 구조화 응답, 일반 텍스트 fallback, 외부 실패 변환
- 학습 점수 0~100 경계, 중복·낮은 신뢰도·알 수 없는 코드
- 빈 학습 이력과 저장 이력 대시보드, 조회 SQL 4개 이하
- 평가 0·100점 경계, 101점 거부, 오답·정답 상태 반영
- 평가 중복 제출 `409`, 빈 답변 `400`

## 3. 브라우저 전체 흐름

실행 주소:

- Frontend: `http://127.0.0.1:5174`
- Backend: `http://127.0.0.1:8080`
- CORS: `http://127.0.0.1:5174`

확인한 흐름:

1. 시작 화면에서 API 연결 상태 `API 연결됨` 확인
2. `8단계최종검증` 사용자와 JPA 관심 기술 생성
3. 첫 대화방 생성
4. `Hibernate가 뭐예요?` 질문 전송
5. USER·ASSISTANT 메시지와 확인 질문 표시
6. 평가와 복습 화면에서 `JPA와 Hibernate 관계` 대상 확인
7. 오답 제출 후 40점, 보완 피드백, 모범 답안 표시
8. 대시보드에서 학습 개념 `1/19`, 복습 필요 `1`, 보완 개념 확인
9. 브라우저 console warning·error 없음

브라우저 검증 후 테스트 사용자와 연관 데이터는 삭제했습니다.

## 4. PostgreSQL 저장 증거

브라우저 흐름 직후 삭제 전에 직접 조회한 결과:

```text
user id=5, nickname=8단계최종검증
chat_rooms=1
chat_messages=2 (USER=1, ASSISTANT=1)
user_concept_statuses=1
assessments=1

skill=JPA
concept=JPA_HIBERNATE_RELATION
understanding_score=0
learning_status=NEEDS_REVIEW
assessment_score=40
correct=false
review_required=true
```

화면의 복습 필요 상태와 DB 값이 일치했습니다.

## 5. MVP 완료 조건

| 완료 조건 | 검증 증거 |
| --- | --- |
| 사용자 관리 | 사용자 API·화면·통합 테스트 |
| 대화방과 메시지 | 소유권 테스트, 브라우저 흐름, DB USER/ASSISTANT 행 |
| AI 멘토 답변 | Fake/OpenAI Client 경계, 구조화·fallback 테스트, 화면 표시 |
| 학습 상태 반영 | 감지·공백 규칙 테스트, DB 개념 상태 |
| 대시보드·학습 현황 | 빈/저장 이력 테스트, 브라우저 집계 화면 |
| 평가·복습 | 평가 경계·중복 테스트, 화면 피드백, DB 평가 행 |
| 실행 가능성 | Docker health, backend test, frontend build·lint |
| 문서 | README, architecture, API, DB, roadmap, progress, 본 검증 기록 |

## 6. 현재 제한과 운영 전 조치

- 실제 OpenAI 호출 코드는 구현했지만 API Key·비용을 사용하는 live 검증은 수행하지 않았습니다.
- 기본 `fake` 모드는 로컬 기능 검증용이며 실제 답변 품질을 대표하지 않습니다.
- 인증이 없는 MVP이므로 `userId`를 클라이언트가 전달합니다. 운영 전 인증·인가가 필요합니다.
- 로컬 스키마는 `ddl-auto=update`입니다. 운영 배포 전 Flyway 또는 Liquibase migration이 필요합니다.
- 모바일용 760px 이하 1열 레이아웃과 입력 버튼 배치를 CSS와 프로덕션 빌드로 검증했습니다. 현재 인앱 브라우저가 요청한 390px 뷰포트를 적용하지 않아 실제 모바일 기기 렌더링은 별도 확인 대상입니다.
- 같은 확인 질문은 한 번만 평가할 수 있으며 추가 평가는 새 AI 대화에서 생성된 질문이 필요합니다.
