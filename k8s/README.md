# NexusChat — Kubernetes Deployment Guide

## Prerequisites
- Kubernetes cluster (minikube, kind, EKS, GKE, AKS)
- `kubectl` configured
- NGINX Ingress Controller installed
- Docker image pushed to a registry

## Directory Structure
```
k8s/
├── namespace.yaml          # nexuschat namespace
├── configmap.yaml          # Non-sensitive config (DB URL, Redis host, etc.)
├── secret.yaml             # Sensitive config (passwords, JWT secret)
├── ingress.yaml            # NGINX Ingress with WebSocket support
├── app/
│   ├── deployment.yaml     # App deployment (2 replicas, rolling update)
│   ├── service.yaml        # ClusterIP service
│   └── hpa.yaml            # HorizontalPodAutoscaler (2-10 replicas)
├── postgres/
│   ├── deployment.yaml     # PostgreSQL (Recreate strategy)
│   ├── service.yaml        # ClusterIP service
│   └── pvc.yaml            # 5Gi PersistentVolumeClaim
├── redis/
│   ├── deployment.yaml     # Redis with AOF persistence
│   └── service.yaml        # ClusterIP service
└── prometheus/
    ├── configmap.yaml      # Prometheus scrape config (K8s SD)
    ├── deployment.yaml     # Prometheus + RBAC
    └── service.yaml        # ClusterIP service
```

## Quick Deploy

### 1. Build and push the Docker image
```bash
docker build -t your-registry/nexuschat:latest .
docker push your-registry/nexuschat:latest
```
Update `k8s/app/deployment.yaml` → `image:` with your registry URL.

### 2. Update secrets
Edit `k8s/secret.yaml` and replace the placeholder values with strong secrets:
```bash
# Generate a strong JWT secret
openssl rand -base64 64
```

### 3. Apply all manifests
```bash
# Apply in order
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/app/
kubectl apply -f k8s/prometheus/
kubectl apply -f k8s/ingress.yaml
```

### 4. Verify deployment
```bash
kubectl get all -n nexuschat
kubectl get pods -n nexuschat
kubectl logs -f deployment/nexuschat-app -n nexuschat
```

### 5. Check health
```bash
kubectl port-forward svc/nexuschat-service 8080:80 -n nexuschat
curl http://localhost:8080/actuator/health
```

## Scaling

The app is designed for horizontal scaling. Redis Pub/Sub keeps all instances in sync — no sticky sessions needed.

```bash
# Manual scale
kubectl scale deployment nexuschat-app --replicas=5 -n nexuschat

# HPA handles auto-scaling based on CPU/memory (configured in hpa.yaml)
kubectl get hpa -n nexuschat
```

## Monitoring

```bash
# Access Prometheus
kubectl port-forward svc/prometheus-service 9090:9090 -n nexuschat
# Open http://localhost:9090
# Query: http_server_requests_seconds_count{namespace="nexuschat"}
```

## Production Checklist
- [ ] Replace `secret.yaml` values with strong secrets (use Sealed Secrets or Vault)
- [ ] Set `storageClassName` in `postgres/pvc.yaml` for your cloud provider
- [ ] Update `ingress.yaml` with your real domain
- [ ] Enable TLS in `ingress.yaml` (cert-manager + Let's Encrypt)
- [ ] Set `imagePullPolicy: IfNotPresent` and pin image tags (not `latest`)
- [ ] Configure Redis PVC instead of `emptyDir` for persistence
- [ ] Set up cluster autoscaler for node-level scaling
