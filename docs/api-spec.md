# DevMentor API 명세

## 1. 공통 규칙

- 기본 경로: `/api`
- Content-Type: `application/json`
- 내부 예외 메시지와 stack trace는 응답에 포함하지 않습니다.

성공 응답:

```json
{
  "success": true,
  "message": "요청이 정상적으로 처리되었습니다.",
  "data": {}
}
```

실패 응답:

```json
{
  "success": false,
  "message": "요청값이 올바르지 않습니다.",
  "data": null
}
```

## 2. Health API

### API 상태 조회

```http
GET /api/health
```

응답 상태: `200 OK`

```json
{
  "success": true,
  "message": "DevMentor API가 정상적으로 실행 중입니다.",
  "data": {
    "status": "UP"
  }
}
```

## 3. 사용자 API

```http
POST /api/users
GET /api/users/{userId}
PUT /api/users/{userId}
```

생성·수정 요청:

```json
{
  "nickname": "주니어",
  "careerYears": 2,
  "currentRole": "백엔드 개발자",
  "learningGoal": "JPA 학습",
  "interestedSkillCodes": ["JAVA", "JPA"]
}
```

## 4. 대화방 API

```http
POST /api/chat-rooms
GET /api/chat-rooms?userId={userId}
GET /api/chat-rooms/{chatRoomId}?userId={userId}
DELETE /api/chat-rooms/{chatRoomId}?userId={userId}
```

생성 요청:

```json
{
  "userId": 1,
  "title": "JPA 학습"
}
```

조회와 삭제는 요청한 사용자에게 속한 대화방만 허용합니다.

## 5. 메시지 API

```http
POST /api/chat-rooms/{chatRoomId}/messages?userId={userId}
GET /api/chat-rooms/{chatRoomId}/messages?userId={userId}
```

저장 요청:

```json
{
  "content": "Hibernate가 뭐예요?"
}
```

AI 답변 생성 성공 응답의 `data`:

```json
{
  "userMessage": {
    "id": 1,
    "role": "USER",
    "content": "Hibernate가 뭐예요?",
    "createdAt": "2026-07-28T10:00:00"
  },
  "assistantMessage": {
    "id": 2,
    "role": "ASSISTANT",
    "content": "Hibernate는 JPA 구현체 중 하나입니다.",
    "createdAt": "2026-07-28T10:00:01"
  },
  "analysis": {
    "answer": "Hibernate는 JPA 구현체 중 하나입니다.",
    "detectedConcepts": [
      {
        "skillCode": "JPA",
        "conceptCode": "JPA_HIBERNATE_RELATION",
        "confidence": 0.9
      }
    ],
    "knowledgeGaps": [],
    "followUpQuestion": "JPA와 Hibernate의 역할 차이를 설명해 보시겠어요?",
    "recommendedConcepts": []
  },
  "structured": true
}
```

- 최근 메시지는 현재 질문을 제외하고 최대 10개를 AI 문맥에 포함합니다.
- JSON 파싱 또는 DTO 검증에 실패하면 `structured=false`, 빈 분석 배열과 일반 텍스트 답변을 반환합니다.
- 사용자 메시지와 AI 메시지는 모두 PostgreSQL에 저장됩니다.
- 외부 AI 호출 전에 사용자 메시지를 별도 트랜잭션으로 저장합니다. 호출 실패 시 질문은 유지되고 AI 메시지는 저장되지 않습니다.

## 6. 학습 추천 API

```http
GET /api/learning/recommendations?userId={userId}
```

응답의 `data`:

```json
[
  {
    "conceptStatusId": 1,
    "skillCode": "JPA",
    "skillName": "JPA",
    "conceptCode": "PERSISTENCE_CONTEXT",
    "conceptName": "영속성 컨텍스트",
    "understandingScore": 10,
    "learningStatus": "LEARNING",
    "reason": "대화에서 개념이 감지되었습니다. (신뢰도 0.95)"
  }
]
```

- 이해도 80점 미만인 저장 상태만 반환합니다.
- `NEEDS_REVIEW`, `LEARNING`, `NOT_STARTED` 순으로 우선하며 같은 상태에서는 낮은 점수와 기술·개념 표시 순서로 정렬합니다.
- AI 감지 신뢰도 0.6 미만과 존재하지 않는 기술·개념 코드는 상태에 반영하지 않습니다.
- 한 AI 응답에 같은 개념이 중복되면 한 번만 반영하고, 감지와 지식 공백이 함께 있으면 지식 공백을 우선합니다.

## 7. 학습 상태 전이 규칙

| 근거 | 점수 변화 | 상태 | 다음 복습 |
| --- | ---: | --- | --- |
| 개념 감지 | `+10` | 80점 미만 `LEARNING`, 이상 `UNDERSTOOD` | 3일 또는 7일 후 |
| 지식 공백 | `-15` | `NEEDS_REVIEW` | 1일 후 |
| 평가 정답 | `+20` | 80점 미만 `LEARNING`, 이상 `UNDERSTOOD` | 3일 또는 7일 후 |
| 평가 오답 | `-20` | `NEEDS_REVIEW` | 1일 후 |

점수는 항상 0~100으로 제한합니다. 평가 정답·오답 규칙은 7단계에서 평가 저장 흐름에 연결합니다.

## 8. 공통 오류

| HTTP 상태 | 조건 | 메시지 원칙 |
| --- | --- | --- |
| `400 Bad Request` | Bean Validation 실패 | 첫 번째 사용자 입력 오류 |
| `404 Not Found` | 도메인 리소스 또는 요청 경로 없음 | 리소스 또는 경로 미존재 안내 |
| `502 Bad Gateway` | 외부 AI 연결·응답 실패 | 재시도를 안내하는 고정 메시지 |
| `500 Internal Server Error` | 처리하지 못한 내부 오류 | 고정된 일반 오류 메시지 |

## 9. 이후 추가 예정

대시보드, 학습 현황, 평가 API는 아직 구현되지 않았습니다. 각 개발 단계에서 실제 요청·응답 DTO와 함께 이 문서를 갱신합니다.
