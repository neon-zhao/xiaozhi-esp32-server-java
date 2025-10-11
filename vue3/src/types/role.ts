/**
 * 角色相关类型定义
 */

// 模型类型
export type ModelType = 'llm' | 'agent'

// 语音提供商类型
export type VoiceProvider = 'edge' | 'aliyun' | 'volcengine' | 'xfyun' | 'minimax'

// 语音性别
export type VoiceGender = '' | 'male' | 'female'

// 角色数据
export interface Role {
  createTime?: string
  updateTime?: string
  userId?: number
  startTime?: string
  endTime?: string
  roleId: number
  avatar?: string
  roleName: string
  roleDesc?: string
  voiceName?: string
  state?: string
  ttsId?: number
  modelId?: number
  modelName?: string
  sttId?: number
  temperature?: number
  topP?: number
  vadEnergyTh?: number
  vadSpeechTh?: number
  vadSilenceTh?: number
  vadSilenceMs?: number
  modelProvider?: string
  ttsProvider?: string
  isDefault?: string
  totalDevice?: number
}

// 角色查询参数
export interface RoleQueryParams {
  start?: number
  limit?: number
  roleName?: string
  isDefault?: number
}

// 角色列表响应
export interface RoleListResponse {
  list: Role[]
  total: number
  pageNum: number
  pageSize: number
  size: number
  startRow: number
  endRow: number
  pages: number
  prePage: number
  nextPage: number
  isFirstPage: boolean
  isLastPage: boolean
  hasPreviousPage: boolean
  hasNextPage: boolean
  navigatePages: number
  navigatepageNums: number[]
  navigateFirstPage: number
  navigateLastPage: number
}

// 语音选项
export interface VoiceOption {
  label: string
  value: string
  gender: VoiceGender
  provider: VoiceProvider
  ttsId?: number // 关联的TTS配置ID
}

// 模型选项（统一LLM和Agent）
export interface ModelOption {
  label: string
  value: number // configId
  desc?: string
  type: ModelType
  provider: string
  // 额外信息
  configName?: string
  configDesc?: string
  agentName?: string
  agentDesc?: string
}

// 语音识别选项
export interface SttOption {
  label: string
  value: number
  desc?: string
}

// 提示词模板
export interface PromptTemplate {
  templateId: number
  templateName: string
  templateContent: string
  isDefault?: boolean |number
}

// 角色表单数据
export interface RoleFormData {
  roleId?: number
  roleName: string
  roleDesc?: string
  avatar?: string
  isDefault: boolean | number // 支持布尔值和数字
  state?: string
  // 模型相关
  modelType: ModelType
  modelId?: number
  temperature?: number
  topP?: number
  // 语音识别相关
  sttId: number
  vadSpeechTh?: number
  vadSilenceTh?: number
  vadEnergyTh?: number
  vadSilenceMs?: number
  // 语音合成相关
  voiceName?: string
  ttsId?: number
  gender?: VoiceGender
}

// 测试语音参数
export interface TestVoiceParams {
  voiceName: string
  ttsId: number
  message: string
  provider: string
}

