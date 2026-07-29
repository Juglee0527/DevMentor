import axios from 'axios'
import type { ApiResponse } from '../types/api'

const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'
export const AI_REQUEST_TIMEOUT_MS = 130_000

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL,
  timeout: 5_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiResponse<null>>(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return '서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
    }
    return error.response?.data.message ?? '서버에 연결할 수 없습니다.'
  }
  return '요청을 처리하지 못했습니다.'
}
