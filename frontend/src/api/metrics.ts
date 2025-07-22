import axios from 'axios';

const ACTUATOR_URL = 'http://localhost:8080/actuator';

// Interfaces for Actuator metrics
export interface MetricNames {
  names: string[];
}

export interface MetricValue {
  name: string;
  description: string;
  baseUnit: string;
  measurements: {
    statistic: string;
    value: number;
  }[];
  availableTags: {
    tag: string;
    values: string[];
  }[];
}

export interface HealthStatus {
  status: string;
  components: {
    [key: string]: {
      status: string;
      details?: any;
    };
  };
}

export interface SystemInfo {
  app: {
    name: string;
    description: string;
    version: string;
  };
  java: {
    version: string;
    vendor: {
      name: string;
    };
    runtime: {
      name: string;
      version: string;
    };
  };
  os: {
    name: string;
    version: string;
    arch: string;
  };
}

// API functions for fetching metrics
export const getMetricNames = async (): Promise<string[]> => {
  const response = await axios.get<MetricNames>(`${ACTUATOR_URL}/metrics`);
  return response.data.names;
};

export const getMetricValue = async (name: string): Promise<MetricValue> => {
  const response = await axios.get<MetricValue>(`${ACTUATOR_URL}/metrics/${name}`);
  return response.data;
};

export const getHealthStatus = async (): Promise<HealthStatus> => {
  const response = await axios.get<HealthStatus>(`${ACTUATOR_URL}/health`);
  return response.data;
};

export const getSystemInfo = async (): Promise<SystemInfo> => {
  const response = await axios.get<SystemInfo>(`${ACTUATOR_URL}/info`);
  return response.data;
};

// Helper function to get all GrepWise custom metrics
export const getGrepWiseMetrics = async (): Promise<MetricValue[]> => {
  const names = await getMetricNames();
  const grepwiseMetrics = names.filter(name => name.startsWith('grepwise.'));
  
  const metricValues = await Promise.all(
    grepwiseMetrics.map(name => getMetricValue(name))
  );
  
  return metricValues;
};

// Helper function to get JVM metrics
export const getJvmMetrics = async (): Promise<MetricValue[]> => {
  const names = await getMetricNames();
  const jvmMetrics = names.filter(name => name.startsWith('jvm.'));
  
  const metricValues = await Promise.all(
    jvmMetrics.map(name => getMetricValue(name))
  );
  
  return metricValues;
};

// Helper function to get system metrics
export const getSystemMetrics = async (): Promise<MetricValue[]> => {
  const names = await getMetricNames();
  const systemMetrics = names.filter(name => 
    name.startsWith('system.') || 
    name.startsWith('process.') || 
    name.startsWith('disk.')
  );
  
  const metricValues = await Promise.all(
    systemMetrics.map(name => getMetricValue(name))
  );
  
  return metricValues;
};

// Helper function to get HTTP metrics
export const getHttpMetrics = async (): Promise<MetricValue[]> => {
  const names = await getMetricNames();
  const httpMetrics = names.filter(name => 
    name.startsWith('http.') || 
    name.startsWith('tomcat.')
  );
  
  const metricValues = await Promise.all(
    httpMetrics.map(name => getMetricValue(name))
  );
  
  return metricValues;
};