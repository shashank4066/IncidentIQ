# Deploy to Azure Kubernetes Service

The repository contains an executable deployment path. No Azure resources are created by cloning it or pushing to GitHub. Terraform apply and a manual deployment incur Azure charges and require your Azure subscription, permissions and DNS/TLS setup.

## 1. Provision AKS and ACR

Install Azure CLI, Terraform, kubectl and kubelogin from their official sources. Authenticate to your intended subscription. Create or identify a Microsoft Entra administrator group and obtain its object ID.

```bash
az login
az account set --subscription YOUR_SUBSCRIPTION_ID
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
# Fill subscription_id, unique registry_name, administrator groups and restricted operator/runner IP CIDRs.
terraform init
terraform plan -out=incidentiq.plan
terraform apply incidentiq.plan
terraform output
```

In PowerShell replace `cp` with `Copy-Item` if needed. Terraform state is sensitive and is ignored. Before shared use, configure a locked, encrypted Azure Storage backend and restrict its access. Do not commit a plan, state or populated tfvars file.

Terraform provisions a resource group, ACR without an admin password, AKS with Entra/Azure RBAC, workload identity support and restricted API-server IP ranges. The kubelet receives `AcrPull`. It does not provision paid AI access or a managed database. Sample PostgreSQL, Redis and Kafka are deployed inside AKS by Kustomize; these single-node examples are not highly available.

## 2. Prepare ingress and stateful configuration

- Install an ingress controller in namespace `ingress-nginx`, with class `nginx`. The supplied NetworkPolicy permits that namespace to reach the frontend.
- Point your hostname to that controller and create a valid TLS secret named `incidentiq-tls` in namespace `incidentiq`. An existing certificate-management solution can supply it. Do not publish Basic-auth traffic without HTTPS.
- Confirm the cluster's default storage class can provision the PostgreSQL/Kafka PVCs. The examples request 10 GiB each.
- For a real operational rollout, replace the sample stateful services with managed equivalents and configure private connectivity, TLS/SASL, backups and retention. The sample broker intentionally uses internal plaintext transport protected by namespace network policies.
- Monitoring is configured in Compose. The base Kubernetes overlay does not install a Prometheus/Grafana/Tempo operator stack. Integrate it with your existing observability platform or add the supplied configuration using your cluster's monitoring conventions.

## 3. Configure GitHub environment `aks`

Create a federated Microsoft Entra application identity for the exact GitHub repository and environment subject (`repo:OWNER/REPOSITORY:environment:aks`). Grant it only the required ACR push and AKS deployment permissions. With Azure RBAC enabled it needs the appropriate AKS data-plane role (for example, a deliberately scoped AKS RBAC administrator role for bootstrapping these namespace resources), plus permission to retrieve cluster credentials. This identity is separate from the kubelet's image-pull identity.

Add environment variables:

| Variable | Value |
| --- | --- |
| `AZURE_REGISTRY_NAME` | ACR name from Terraform |
| `AZURE_RESOURCE_GROUP` | Resource group from Terraform |
| `AZURE_AKS_NAME` | AKS cluster name |
| `APP_HOST` | Real HTTPS hostname, without a scheme |

Add environment secrets:

`AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `DATABASE_PASSWORD`, `REDIS_PASSWORD`, `ADMIN_PASSWORD`, `VIEWER_PASSWORD`, `WORKER_PASSWORD`, and optionally `AI_API_KEY`.

Use independently generated values. A changed database password must also be rotated inside an existing PostgreSQL volume; replacing only a Kubernetes Secret does not change database users. For a non-default AI endpoint/model, update the ConfigMap before deploying; never put keys in that ConfigMap.

The deployment workflow targets a Linux self-hosted runner labeled `incidentiq-deploy` with Docker, Azure CLI, Python and a stable egress IP in the AKS authorized ranges. Use an isolated trusted runner and do not run untrusted pull requests on it. Restrict the `aks` environment to trusted deployment branches and optionally require a reviewer.

## 4. Deploy

First run **Verify IncidentIQ** and inspect its result. Then run **Deploy to AKS** manually from the intended commit. It authenticates through OIDC, builds SHA-tagged images, pushes to ACR, creates the application Secret, applies the AKS overlay and waits for each Deployment rollout. The final check requests your HTTPS `/healthz` endpoint.

The base configuration uses `SEED_DEMO=false`. Sign in and register services, then connect real telemetry/alert producers. The default two API replicas share PostgreSQL, Redis and the outbox. Flyway applies migrations under its database migration lock.

## Manual deployment alternative

Build/push the three images with the included Dockerfiles, replace `REPLACE_REGISTRY.azurecr.io` and `REPLACE_TAG` in the AKS overlay, and replace the example hostname. Create namespace and the application Secret from a local env file using `kubectl create secret generic ... --from-env-file=... --dry-run=client -o yaml | kubectl apply -f -`, then apply `kubectl apply -k infra/k8s/overlays/aks`.

Do not apply the overlay unchanged: placeholder images and `incidentiq.example.com` are intentionally non-deploying values. The automated workflow replaces them from your actual configuration.

## Rollback and cleanup

Use `kubectl -n incidentiq rollout history deployment/incident-service` and `rollout undo` for application rollback, or redeploy the prior known-good SHA. Database changes require compatible forward migrations; do not assume rolling back code reverses schema changes.

When finished with a disposable environment, inspect your Terraform state and run `terraform destroy` from the same configured directory after reviewing the plan. PVC data, external DNS/certificates, registry images and resources created outside Terraform need separate lifecycle decisions. Never delete operational data as part of routine rollback.
