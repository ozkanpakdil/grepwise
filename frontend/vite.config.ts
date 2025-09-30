import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { VitePWA } from 'vite-plugin-pwa';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [
    react(),
    // Enable PWA only in production to avoid SW caching interfering with Vite dev server
    ...(mode === 'production'
      ? [
          VitePWA({
            registerType: 'autoUpdate',
            includeAssets: ['favicon.svg'],
            devOptions: { enabled: false },
            manifest: {
              name: 'GrepWise',
              short_name: 'GrepWise',
              description: 'An open-source alternative to Splunk for log analysis and monitoring',
              theme_color: '#0f172a',
              icons: [
                {
                  src: '/icons/icon-192x192.png',
                  sizes: '192x192',
                  type: 'image/png',
                  purpose: 'any maskable',
                },
                {
                  src: '/icons/icon-512x512.png',
                  sizes: '512x512',
                  type: 'image/png',
                  purpose: 'any maskable',
                },
              ],
            },
            workbox: {
              globPatterns: ['**/*.{js,css,html,ico,png,svg}'],
              runtimeCaching: [
                {
                  urlPattern: /^https:\/\/api\.grepwise\.com\/.*/i,
                  handler: 'NetworkFirst',
                  options: {
                    cacheName: 'api-cache',
                    expiration: {
                      maxEntries: 100,
                      maxAgeSeconds: 60 * 60 * 24, // 24 hours
                    },
                    networkTimeoutSeconds: 10,
                  },
                },
              ],
            },
          }),
        ]
      : []),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
}));
