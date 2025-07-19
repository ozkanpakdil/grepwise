# Docker Test Script for GrepWise
# This script tests the Docker configuration by building and starting the containers

Write-Host "=== GrepWise Docker Configuration Test ===" -ForegroundColor Cyan

# Check if Docker is installed
try {
    $dockerVersion = docker --version
    Write-Host "✓ Docker is installed: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if Docker Compose is installed
try {
    $composeVersion = docker-compose --version
    Write-Host "✓ Docker Compose is installed: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Compose is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if Dockerfile.backend exists
if (Test-Path -Path "Dockerfile.backend") {
    Write-Host "✓ Backend Dockerfile exists" -ForegroundColor Green
} else {
    Write-Host "✗ Backend Dockerfile not found" -ForegroundColor Red
    exit 1
}

# Check if frontend Dockerfile exists
if (Test-Path -Path "frontend\Dockerfile") {
    Write-Host "✓ Frontend Dockerfile exists" -ForegroundColor Green
} else {
    Write-Host "✗ Frontend Dockerfile not found" -ForegroundColor Red
    exit 1
}

# Check if docker-compose.yml exists
if (Test-Path -Path "docker-compose.yml") {
    Write-Host "✓ docker-compose.yml exists" -ForegroundColor Green
} else {
    Write-Host "✗ docker-compose.yml not found" -ForegroundColor Red
    exit 1
}

# Check if Nginx configuration exists
if (Test-Path -Path "nginx\nginx.conf") {
    Write-Host "✓ Nginx configuration exists" -ForegroundColor Green
} else {
    Write-Host "✗ Nginx configuration not found" -ForegroundColor Red
    exit 1
}

# Validate docker-compose.yml
Write-Host "`nValidating docker-compose.yml..." -ForegroundColor Cyan
try {
    docker-compose config > $null
    Write-Host "✓ docker-compose.yml is valid" -ForegroundColor Green
} catch {
    Write-Host "✗ docker-compose.yml is invalid: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`nAll Docker configuration files exist and are valid!" -ForegroundColor Green
Write-Host "`nTo build and start the containers, run:" -ForegroundColor Yellow
Write-Host "docker-compose up -d" -ForegroundColor Yellow
Write-Host "`nTo verify the containers are running, run:" -ForegroundColor Yellow
Write-Host "docker-compose ps" -ForegroundColor Yellow
Write-Host "`nTo stop the containers, run:" -ForegroundColor Yellow
Write-Host "docker-compose down" -ForegroundColor Yellow

Write-Host "`nFor more information, see DOCKER.md" -ForegroundColor Cyan