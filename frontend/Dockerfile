# Frontend Dockerfile for GrepWise
# Multi-stage build for optimized image size

# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package.json and package-lock.json (or yarn.lock) first to leverage Docker cache
COPY package*.json ./
# If using Yarn, uncomment the next line and comment the npm install line
# COPY package.json yarn.lock ./
# RUN yarn install --frozen-lockfile

# Install dependencies
RUN npm ci

# Copy the rest of the frontend code
COPY . .

# Build the application
RUN npm run build

# Runtime stage using Nginx
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy the built assets from the build stage
COPY --from=build /app/dist .

# Copy custom nginx configuration if needed
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Configure container to run as nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

USER nginx

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]