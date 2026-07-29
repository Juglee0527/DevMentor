import { apiClient } from './client'
import type { AiFeedback, AiFeedbackRating, ApiResponse } from '../types/api'

export async function submitAiFeedback(
  userId: number,
  chatMessageId: number,
  rating: AiFeedbackRating,
  correctedAnswer: string,
  trainingConsent: boolean,
): Promise<AiFeedback> {
  const response = await apiClient.post<ApiResponse<AiFeedback>>('/ai-feedback', {
    userId,
    chatMessageId,
    rating,
    correctedAnswer: correctedAnswer.trim() || null,
    trainingConsent,
  })
  return response.data.data
}
