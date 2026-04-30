export type DocumentDTO = {
  id: string
  title?: string
  description?: string
  filePath?: string
  fileSize?: number
  mimeType?: string
  workflowId?: string
  currentStep?: number
  status?: string
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  completedAt?: string
}
