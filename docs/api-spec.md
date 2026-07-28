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

## 3. 공통 오류

| HTTP 상태 | 조건 | 메시지 원칙 |
| --- | --- | --- |
| `400 Bad Request` | Bean Validation 실패 | 첫 번째 사용자 입력 오류 |
| `404 Not Found` | 도메인 리소스 또는 요청 경로 없음 | 리소스 또는 경로 미존재 안내 |
| `500 Internal Server Error` | 처리하지 못한 내부 오류 | 고정된 일반 오류 메시지 |

## 4. 이후 추가 예정

사용자, 대화방, 메시지, 학습 상태, 평가 API는 아직 구현되지 않았습니다. 각 개발 단계에서 실제 요청·응답 DTO와 함께 이 문서를 갱신합니다.

