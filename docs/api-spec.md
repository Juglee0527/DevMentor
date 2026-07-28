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

3단계에서는 `USER` 메시지만 저장합니다. AI 답변은 4단계에서 같은 endpoint 응답 계약을 확장합니다.

## 6. 공통 오류

| HTTP 상태 | 조건 | 메시지 원칙 |
| --- | --- | --- |
| `400 Bad Request` | Bean Validation 실패 | 첫 번째 사용자 입력 오류 |
| `404 Not Found` | 도메인 리소스 또는 요청 경로 없음 | 리소스 또는 경로 미존재 안내 |
| `500 Internal Server Error` | 처리하지 못한 내부 오류 | 고정된 일반 오류 메시지 |

## 7. 이후 추가 예정

AI 응답, 학습 상태, 대시보드, 평가 API는 아직 구현되지 않았습니다. 각 개발 단계에서 실제 요청·응답 DTO와 함께 이 문서를 갱신합니다.
