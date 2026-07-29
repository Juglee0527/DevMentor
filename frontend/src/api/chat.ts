import { AI_REQUEST_TIMEOUT_MS, apiClient } from './client'
import type { ApiResponse, ChatExchange, ChatMessage, ChatRoom } from '../types/api'

export async function createChatRoom(
  userId: number,
  title: string,
): Promise<ChatRoom> {
  const response = await apiClient.post<ApiResponse<ChatRoom>>('/chat-rooms', {
    userId,
    title,
  })
  return response.data.data
}

export async function getChatRooms(userId: number): Promise<ChatRoom[]> {
  const response = await apiClient.get<ApiResponse<ChatRoom[]>>('/chat-rooms', {
    params: { userId },
  })
  return response.data.data
}

export async function getMessages(
  roomId: number,
  userId: number,
): Promise<ChatMessage[]> {
  const response = await apiClient.get<ApiResponse<ChatMessage[]>>(
    `/chat-rooms/${roomId}/messages`,
    { params: { userId } },
  )
  return response.data.data
}

export async function sendMessage(
  roomId: number,
  userId: number,
  content: string,
): Promise<ChatExchange> {
  const response = await apiClient.post<ApiResponse<ChatExchange>>(
    `/chat-rooms/${roomId}/messages`,
    { content },
    {
      params: { userId },
      timeout: AI_REQUEST_TIMEOUT_MS,
    },
  )
  return response.data.data
}

export async function deleteChatRoom(
  roomId: number,
  userId: number,
): Promise<void> {
  await apiClient.delete(`/chat-rooms/${roomId}`, { params: { userId } })
}
