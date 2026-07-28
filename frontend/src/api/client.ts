import axios from 'axios'

const DEFAULT_API_BASE_URL = 'http://localhost:8080/api'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL,
  timeout: 5_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

