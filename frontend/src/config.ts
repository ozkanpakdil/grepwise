export const config = {
    apiBaseUrl: 'http://localhost:8080',
    apiPaths: {
        logs: '/api/logs',
    },
    defaults: {
        pageSize: 100,
        refreshInterval: '30s',
    },
};

export const apiUrl = (path: string) => `${config.apiBaseUrl}${path}`;
