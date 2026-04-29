import axios from 'axios'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: `${API_URL}/api`,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token')
  if (token && config.headers) {
    // @ts-ignore
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export const login = async (email: string, password: string) => {
  const response = await api.post('/auth/login', { email, password })
  return response.data
}

export const getPendingDocuments = async () => {
  const response = await api.get('/documents/pending')
  return response.data
}

export const getDocument = async (id: string) => {
  const response = await api.get(`/documents/${id}`)
  return response.data
}

export const getDocumentViewUrl = async (id: string) => {
  return `${API_URL}/api/documents/${id}/view`
}

export const uploadDocument = async (
  file: File,
  workflowId: string,
  title: string,
  description?: string
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('workflowId', workflowId)
  formData.append('title', title)
  if (description) formData.append('description', description)

  const response = await api.post('/documents/upload', formData, {
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

export const createWorkflow = async (workflow: any) => {
  const response = await api.post('/workflows', workflow)
  return response.data
}

export default api
