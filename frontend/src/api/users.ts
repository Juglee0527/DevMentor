import { apiClient } from './client'
import type { ApiResponse, User } from '../types/api'

export interface UserPayload {
  nickname: string
  careerYears: number
  currentRole: string
  learningGoal: string
  interestedSkillCodes: string[]
}

export async function createUser(payload: UserPayload): Promise<User> {
  const response = await apiClient.post<ApiResponse<User>>('/users', payload)
  return response.data.data
}

export async function getUser(userId: number): Promise<User> {
  const response = await apiClient.get<ApiResponse<User>>(`/users/${userId}`)
  return response.data.data
}
