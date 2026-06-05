# 🚀 NexusChat Deployment Guide

## Prerequisites

Before deploying NexusChat, you need:

### For Docker Deployment
- Docker Hub account
- Docker installed locally

### For Kubernetes Deployment
- Kubernetes cluster (EKS, GKE, AKS, or local with minikube)
- `kubectl` installed and configured
- Docker Hub account for image registry

---

## 📦 GitHub Secrets Configuration

Add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

### Required Secrets

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `DOCKER_USERNAME` | Your Docker Hub username | `your-dockerhub-username` |
| `DOCKER_PASSWORD` | Your Docker Hub password or access token | `dckr_pat_xxxxx` |
| `KUBE_CONFIG` | Base64-encoded kubeconfig file (for K8s deployment) | `base64 ~/.kube/config` |

### Optional Secrets

| Secret Name | Description |
|-------------|-------------|
| `SLACK_WEBHOOK_URL` | Slack webhook for deployment notifications |

---

## 🐳 Docker Deployment

### Manual Deployment

```bash
# Build the image
docker build -t your-username/nexuschat:latest .

# Push to Docker Hub
docker login
docker push your-username/nexuschat:latest

# Run with docker-compose
docker-compose up -d
```

### Automated Deployment (CI/CD)

The `docker-publish.yml` workflow automatically:
1. Builds Docker image on every push to `master`
2. Pushes to Docker Hub with multiple tags:
   - `latest` (for master branch)
   - `<branch-name>` (for feature branches)
   - `<commit-sha>` (for specific commits)
   - `v1.2.3` (for version tags)

**Trigger:** Push to `master` or create a tag

```bash
git tag v6.0.0
git push origin v6.0.0
```

---

## ☸️ Kubernetes Deployment

### 1. Configure kubectl

```bash
# For cloud providers
# AWS EKS
aws eks update-kubeconfig --name your-cluster-name --region us-east-1

# Google GKE
gcloud container clusters get-credentials your-cluster-name --zone us-central1-a

# Azure AKS
az aks get-credentials --resource-group your-rg --name your-cluster-name
```

### 2. Create Secrets

```bash
# Database password
kubectl create secret generic nexuschat-secret \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-db-password \
  --from-literal=JWT_SECRET=your-jwt-secret-256-bits-minimum \
  -n nexuschat

# Docker registry secret (if using private registry)
kubectl create secret docker-registry regcred \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email \
  -n nexuschat
```

### 3. Update ConfigMap

Edit `k8s/configmap.yaml` with your configuration:

```yaml
data:
  SPRING_DATASOURCE_URL: jdbc:postgresql://nexuschat-postgres:5432/nexuschat
  SPRING_DATASOURCE_USERNAME: nexuschat
  SPRING_DATA_REDIS_HOST: nexuschat-redis
  # ... other configs
```

### 4. Deploy

```bash
# Deploy everything
kubectl apply -f k8s/

# Or deploy step-by-step
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/rabbitmq/
kubectl apply -f k8s/app/
kubectl apply -f k8s/ingress.yaml
```

### 5. Verify Deployment

```bash
# Check pods
kubectl get pods -n nexuschat

# Check services
kubectl get svc -n nexuschat

# Check logs
kubectl logs -f deployment/nexuschat-app -n nexuschat

# Check health
kubectl port-forward svc/nexuschat-service 8080:8080 -n nexuschat
curl http://localhost:8080/actuator/health
```

---

## 🔄 Automated K8s Deployment (CI/CD)

The `cd.yml` workflow automatically:
1. Builds and pushes Docker image
2. Updates K8s manifests with new image tag
3. Deploys to Kubernetes cluster
4. Verifies deployment

**Prerequisites:**
1. Add `KUBE_CONFIG` secret to GitHub (base64-encoded kubeconfig)
   ```bash
   cat ~/.kube/config | base64 | pbcopy
   ```
2. Push to `master` branch

The workflow will:
- ✅ Build multi-arch Docker image (amd64, arm64)
- ✅ Push to Docker Hub with proper tags
- ✅ Update K8s deployment with new image
- ✅ Rollout the deployment
- ✅ Verify all pods are running

---

## 🌐 Domain & Ingress Setup

### Using Nginx Ingress

```bash
# Install nginx ingress controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# Update k8s/ingress.yaml with your domain
# Then apply
kubectl apply -f k8s/ingress.yaml
```

### Configure DNS

Point your domain to the ingress controller's external IP:

```bash
kubectl get svc -n ingress-nginx
# Get EXTERNAL-IP and create A record
```

Example DNS record:
```
Type: A
Name: nexuschat.yourdomain.com
Value: <EXTERNAL-IP>
```

---

## 🔐 SSL/TLS Certificate

### Using cert-manager (Let's Encrypt)

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF

# Update ingress with TLS
# Add this to k8s/ingress.yaml:
#   tls:
#   - hosts:
#     - nexuschat.yourdomain.com
#     secretName: nexuschat-tls
#   annotations:
#     cert-manager.io/cluster-issuer: "letsencrypt-prod"
```

---

## 📊 Monitoring & Observability

### Prometheus Metrics

NexusChat exposes Prometheus metrics at `/actuator/prometheus`

```bash
# Port forward to access metrics
kubectl port-forward svc/nexuschat-service 8080:8080 -n nexuschat

# Query metrics
curl http://localhost:8080/actuator/prometheus
```

### Grafana Dashboard

1. Install Prometheus & Grafana in your cluster
2. Import NexusChat dashboard (dashboards available in `/monitoring/grafana/`)
3. Configure Prometheus to scrape NexusChat pods

---

## 🔄 Rollback Strategy

If deployment fails:

```bash
# Check deployment history
kubectl rollout history deployment/nexuschat-app -n nexuschat

# Rollback to previous version
kubectl rollout undo deployment/nexuschat-app -n nexuschat

# Rollback to specific revision
kubectl rollout undo deployment/nexuschat-app --to-revision=2 -n nexuschat
```

---

## 🧪 Testing Deployment

```bash
# Health check
curl https://nexuschat.yourdomain.com/actuator/health

# Test WebSocket connection
# Use the web UI or a WebSocket client

# Check metrics
curl https://nexuschat.yourdomain.com/actuator/prometheus

# Test authentication
curl -X POST https://nexuschat.yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'
```

---

## 📝 Troubleshooting

### Pod not starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n nexuschat

# Check logs
kubectl logs <pod-name> -n nexuschat

# Check events
kubectl get events -n nexuschat --sort-by='.lastTimestamp'
```

### Database connection issues

```bash
# Test postgres connectivity
kubectl run -it --rm debug --image=postgres:16-alpine --restart=Never -n nexuschat -- \
  psql -h nexuschat-postgres -U nexuschat -d nexuschat
```

### Redis connection issues

```bash
# Test redis connectivity
kubectl run -it --rm debug --image=redis:7-alpine --restart=Never -n nexuschat -- \
  redis-cli -h nexuschat-redis ping
```

---

## 🎯 Production Checklist

Before deploying to production:

- [ ] Update all secrets (database password, JWT secret)
- [ ] Configure proper resource limits in K8s manifests
- [ ] Set up SSL/TLS certificates
- [ ] Configure proper ingress with your domain
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy for database
- [ ] Test rollback procedure
- [ ] Configure log aggregation (ELK/Loki)
- [ ] Set up proper RBAC in K8s cluster
- [ ] Review security settings (network policies, pod security policies)
- [ ] Configure HPA (Horizontal Pod Autoscaler) thresholds
- [ ] Set up disaster recovery plan

---

## 📞 Support

For deployment issues:
- Check GitHub Issues
- Review logs: `kubectl logs -f deployment/nexuschat-app -n nexuschat`
- Check health endpoint: `/actuator/health`
