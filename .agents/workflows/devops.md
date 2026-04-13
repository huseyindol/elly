---
description: DevOps / K8s persona — Cursor ve genel agent’lar için (Claude ile eşlenik)
---

# Workflow: DevOps Engineer

**Kanonik tanım:** [`.claude/agents/devops-engineer.md`](../../.claude/agents/devops-engineer.md)  
**İlgili komut:** [`.claude/commands/k8s-deploy.md`](../../.claude/commands/k8s-deploy.md)

## Ne zaman kullan

- Pod / Service / Ingress / Secret sorunları
- GitHub Actions (`deploy.yml`, `rollback.yml`)
- Docker imajı, Actuator, monitoring

## Cursor’da kısa talimat

> devops-engineer rolünde: önce `k8s/` ve ilgili workflow dosyalarını oku; kubectl önerilerini tam ve çalıştırılabilir yaz.

## Odak dizinler

- `k8s/`
- `.github/workflows/`
- `docker/` (varsa proje özelinde)
