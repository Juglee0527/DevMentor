import { apiClient } from './client'
import type { ApiResponse, Dashboard, LearningStatusOverview } from '../types/api'

export async function getDashboard(userId: number): Promise<Dashboard> {
  const response = await apiClient.get<ApiResponse<Dashboard>>('/dashboard', {
    params: { userId },
  })
  return response.data.data
}

export async function getLearningStatus(
  userId: number,
): Promise<LearningStatusOverview> {
  const response = await apiClient.get<ApiResponse<LearningStatusOverview>>(
    '/learning/status',
    { params: { userId } },
  )
  return response.data.data
}
