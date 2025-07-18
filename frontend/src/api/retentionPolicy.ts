import axios from 'axios';

const API_URL = 'http://localhost:8080/api/config/retention-policies';

export interface RetentionPolicy {
  id?: string;
  name: string;
  maxAgeDays: number;
  enabled: boolean;
  applyToSources?: string[];
}

export const getRetentionPolicies = async (): Promise<RetentionPolicy[]> => {
  const response = await axios.get<RetentionPolicy[]>(API_URL);
  return response.data;
};

export const getRetentionPolicy = async (id: string): Promise<RetentionPolicy> => {
  const response = await axios.get<RetentionPolicy>(`${API_URL}/${id}`);
  return response.data;
};

export const createRetentionPolicy = async (policy: RetentionPolicy): Promise<RetentionPolicy> => {
  const response = await axios.post<RetentionPolicy>(API_URL, policy);
  return response.data;
};

export const updateRetentionPolicy = async (id: string, policy: RetentionPolicy): Promise<RetentionPolicy> => {
  const response = await axios.put<RetentionPolicy>(`${API_URL}/${id}`, policy);
  return response.data;
};

export const deleteRetentionPolicy = async (id: string): Promise<void> => {
  await axios.delete(`${API_URL}/${id}`);
};

export const applyRetentionPolicy = async (id: string): Promise<number> => {
  const response = await axios.post<number>(`${API_URL}/${id}/apply`);
  return response.data;
};

export const applyAllRetentionPolicies = async (): Promise<number> => {
  const response = await axios.post<number>(`${API_URL}/apply-all`);
  return response.data;
};