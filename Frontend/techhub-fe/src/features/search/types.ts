export interface CategoryConfig {
  id: string;
  name: string;
  subcategories: { id: string; name: string }[];
  attributes: {
    key: string;
    label: string;
    options: string[];
  }[];
}
