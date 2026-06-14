export interface AssistantRequest {
  message: string;
}

export interface AssistantResponse {
  reply: string;
  suggestions: string[];
}
