import { apiClient } from './client'
import type { ApiResponse } from '../types/api'

interface HealthData {
  status: string
}

export async function getApiHealth(): Promise<ApiResponse<HealthData>> {
  const response =
    await apiClient.get<ApiResponse<HealthData>>('/health')

  return response.data
}

