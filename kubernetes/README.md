# GrepWise Kubernetes Deployment

This directory contains Kubernetes manifests for deploying GrepWise to a Kubernetes cluster.

## Prerequisites

- Kubernetes cluster (v1.19+)
- kubectl configured to communicate with your cluster
- Docker images for GrepWise backend and frontend

## Building Docker Images

Before deploying to Kubernetes, you need to build the Docker images for the backend and frontend:

```bash
# Build backend image
docker build -t grepwise-backend:latest -f Dockerfile.backend .

# Build frontend image
cd frontend
docker build -t grepwise-frontend:latest .
cd ..
```

## Deploying to Kubernetes

1. Create the namespace:

```bash
kubectl apply -f namespace.yaml
```

2. Create the persistent volume claims:

```bash
kubectl apply -f persistent-volumes.yaml
```

3. Create the Nginx ConfigMap:

```bash
kubectl apply -f nginx-configmap.yaml
```

4. Deploy the backend:

```bash
kubectl apply -f backend-deployment.yaml
kubectl apply -f backend-service.yaml
```

5. Deploy the frontend:

```bash
kubectl apply -f frontend-deployment.yaml
kubectl apply -f frontend-service.yaml
```

6. Check the deployment status:

```bash
kubectl get all -n grepwise
```

## Accessing the Application

Once the deployment is complete, you can access the application through the frontend service's external IP:

```bash
kubectl get svc -n grepwise grepwise-frontend
```

Use the EXTERNAL-IP from the output to access the application in your browser.

## Scaling the Application

To scale the backend or frontend, you can use the `kubectl scale` command:

```bash
# Scale backend to 3 replicas
kubectl scale deployment -n grepwise grepwise-backend --replicas=3

# Scale frontend to 2 replicas
kubectl scale deployment -n grepwise grepwise-frontend --replicas=2
```

## Customizing the Deployment

### Resource Limits

You can adjust the CPU and memory limits in the deployment files based on your cluster's capacity and the application's requirements.

### Storage

The persistent volume claims use the "standard" storage class. You may need to adjust this based on your cluster's available storage classes.

### Service Type

The frontend service uses LoadBalancer type, which requires a cloud provider that supports load balancers. For on-premises clusters, you might want to use NodePort or Ingress instead.

## Troubleshooting

### Checking Logs

```bash
# Check backend logs
kubectl logs -n grepwise deployment/grepwise-backend

# Check frontend logs
kubectl logs -n grepwise deployment/grepwise-frontend
```

### Checking Pod Status

```bash
kubectl describe pod -n grepwise -l app=grepwise
```

## Cleanup

To remove all resources created by this deployment:

```bash
kubectl delete namespace grepwise
```