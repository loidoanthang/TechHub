export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message: string;
  metadata?: unknown;
}

export interface PageResponse<T = unknown> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface ApiErrorResponse {
  success: boolean;
  message: string;
  errorCode?: string;
  data?: unknown;
  metadata?: unknown;
}
