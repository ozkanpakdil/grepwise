import axios from 'axios';

const API_URL = 'http://localhost:8080/api/config/log-directories';

export interface LogDirectoryConfig {
  id?: string;
  directoryPath: string;
  filePattern: string;
  scanIntervalSeconds: number;
}

export const getLogDirectoryConfigs = async (): Promise<LogDirectoryConfig[]> => {
  const response = await axios.get<LogDirectoryConfig[]>(API_URL);
  return response.data;
};

export const getLogDirectoryConfig = async (id: string): Promise<LogDirectoryConfig> => {
  const response = await axios.get<LogDirectoryConfig>(`${API_URL}/${id}`);
  return response.data;
};

export const createLogDirectoryConfig = async (config: LogDirectoryConfig): Promise<LogDirectoryConfig> => {
  const response = await axios.post<LogDirectoryConfig>(API_URL, config);
  return response.data;
};

export const updateLogDirectoryConfig = async (id: string, config: LogDirectoryConfig): Promise<LogDirectoryConfig> => {
  const response = await axios.put<LogDirectoryConfig>(`${API_URL}/${id}`, config);
  return response.data;
};

export const deleteLogDirectoryConfig = async (id: string): Promise<void> => {
  await axios.delete(`${API_URL}/${id}`);
};

export const scanLogDirectory = async (id: string): Promise<number> => {
  const response = await axios.post<number>(`${API_URL}/${id}/scan`);
  return response.data;
};

export const scanAllLogDirectories = async (): Promise<number> => {
  const response = await axios.post<number>(`${API_URL}/scan-all`);
  return response.data;
};
