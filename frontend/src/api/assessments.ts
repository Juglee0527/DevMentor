import { AI_REQUEST_TIMEOUT_MS, apiClient } from './client'
import type { ApiResponse, AssessmentResult, ReviewTarget } from '../types/api'

export async function getReviewTargets(userId: number): Promise<ReviewTarget[]> {
  const response = await apiClient.get<ApiResponse<ReviewTarget[]>>('/reviews', {
    params: { userId },
  })
  return response.data.data
}

export async function getAssessments(userId: number): Promise<AssessmentResult[]> {
  const response = await apiClient.get<ApiResponse<AssessmentResult[]>>(
    '/assessments',
    { params: { userId } },
  )
  return response.data.data
}

export async function submitAssessment(
  target: ReviewTarget,
  userId: number,
  userAnswer: string,
): Promise<AssessmentResult> {
  const response = await apiClient.post<ApiResponse<AssessmentResult>>(
    '/assessments',
    {
      userId,
      chatMessageId: target.chatMessageId,
      skillCode: target.skillCode,
      conceptCode: target.conceptCode,
      userAnswer,
    },
    { timeout: AI_REQUEST_TIMEOUT_MS },
  )
  return response.data.data
}
