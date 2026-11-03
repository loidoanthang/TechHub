import { CategoryAttributeDataType, CategoryStatus } from "./enums";

export interface CategorySummaryResponse {
  id: string;
  name: string;
  description: string | null;
  status: CategoryStatus;
  attributesCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryAttributeResponse {
  id: string;
  name: string;
  dataType: CategoryAttributeDataType;
  options: string[];
  sampleValue?: string | null;
  createdAt: string;
  updatedAt: string;
}
