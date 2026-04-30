import axios from 'axios'
import type { AxiosError } from 'axios'
import type { DocumentDTO } from '../types'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export type LoginResponse = {
  accessToken?: string
  access_token?: string
  tokenType?: string
  email?: string
  fullName?: string
  role?: string
}

export type ApiError = {
  message: string
  status?: number
}

const api = axios.create({
  baseURL: `${API_URL}/api`,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function getErrorMessage(error: unknown, fallback = 'Unexpected error') {
  const axiosError = error as AxiosError<{ message?: string }>
  return axiosError.response?.data?.message || axiosError.message || fallback
}

export const login = async (email: string, password: string): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>('/auth/login', { email, password })
  return response.data
}

export const getPendingDocuments = async (): Promise<DocumentDTO[]> => {
  const response = await api.get<DocumentDTO[]>('/documents/pending')
  return response.data
}

export const getDocument = async (id: string): Promise<DocumentDTO> => {
  const response = await api.get<DocumentDTO>(`/documents/${id}`)
  return response.data
}

export const getDocumentViewUrl = (id: string) => {
  return `${API_URL}/api/documents/${id}/view`
}

export const uploadDocument = async (
  file: File,
  workflowId: string,
  title: string,
  description?: string
): Promise<DocumentDTO> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('workflowId', workflowId)
  formData.append('title', title)
  if (description) formData.append('description', description)

  const response = await api.post<DocumentDTO>('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const prepareSignature = async (documentId: string) => {
  const response = await api.post('/signatures/prepare', { documentId })
  return response.data
}

export const completeSignature = async (
  documentId: string,
  signatureValue: string,
  comments?: string
) => {
  const response = await api.post('/signatures/complete', {
    documentId,
    signatureValue,
    comments,
  })
  return response.data
}

export const rejectDocument = async (documentId: string, reason: string) => {
  await api.post('/signatures/reject', { documentId, reason })
}

export const getWorkflows = async () => {
  const response = await api.get('/workflows')
  return response.data
}

export const createWorkflow = async (workflow: unknown) => {
  const response = await api.post('/workflows', workflow)
  return response.data
}

export default api
