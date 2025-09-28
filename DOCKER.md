# Docker Setup for GrepWise

This document provides instructions for running GrepWise using Docker containers.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) (version 20.10.0 or higher)
- [Docker Compose](https://docs.docker.com/compose/install/) (version 2.0.0 or higher)

## Quick Start

To start the entire application stack:

```bash
docker-compose up -d
```

This will:
- Build the backend and frontend Docker images (if not already built)
- Start the backend service on port 8080
- Start the frontend service on port 80
- Create necessary volumes for persistent data

Access the application at: http://localhost

## Development Environment

For development purposes, you can use the following commands:

### Build the images

```bash
docker-compose build
```

### Start only the backend

```bash
docker-compose up -d backend
```

### Start only the frontend

```bash
docker-compose up -d frontend
```

### View logs

```bash
# View all logs
docker-compose logs -f

# View only backend logs
docker-compose logs -f backend

# View only frontend logs
docker-compose logs -f frontend
```

### Stop the services

```bash
docker-compose down
```

## Production Deployment

For production deployment, consider the following:

1. Update environment variables in docker-compose.yml for production settings
2. Use Docker volumes or bind mounts for persistent data
3. Configure proper networking and security settings
4. Set up a reverse proxy (like Nginx or Traefik) for SSL termination

Example production docker-compose command:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Container Details

### Backend Container

- Base image: Eclipse Temurin JRE 25 Alpine
- Exposed port: 8080
- Volumes:
  - `./logs:/app/logs` - Application logs
  - `./lucene-index:/app/lucene-index` - Lucene search index
  - `backend-data:/app/data` - Persistent application data

### Frontend Container

- Base image: Nginx Alpine
- Exposed port: 80
- Configuration:
  - Custom Nginx configuration for serving the React app
  - API proxying to the backend service

## Data Persistence

The following data is persisted:

- **Logs**: Stored in the `./logs` directory on the host
- **Lucene Index**: Stored in the `./lucene-index` directory on the host
- **Application Data**: Stored in a Docker volume named `backend-data`

## Troubleshooting

### Common Issues

1. **Port conflicts**: If ports 8080 or 80 are already in use, modify the port mappings in docker-compose.yml
2. **Permission issues**: Ensure proper permissions for mounted volumes
3. **Container not starting**: Check logs with `docker-compose logs -f`

### Health Checks

The backend container includes a health check that verifies the application is running correctly. You can check the health status with:

```bash
docker ps
```

Look for the "healthy" status in the output.

## Testing the Docker Setup

To verify the Docker setup is working correctly:

1. Start the containers: `docker-compose up -d`
2. Check container status: `docker ps`
3. Access the frontend: http://localhost
4. Test the API: http://localhost/api/health (should be proxied to the backend)

## Customization

You can customize the Docker setup by:

1. Modifying environment variables in docker-compose.yml
2. Updating the Nginx configuration in nginx/nginx.conf
3. Adjusting JVM options in the backend Dockerfile

## Cleanup

To remove all containers, networks, and volumes:

```bash
docker-compose down -v
```

To remove only the containers and networks but keep the volumes:

```bash
docker-compose down
```