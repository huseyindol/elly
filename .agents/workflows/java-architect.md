---
description: Java / Spring mimari persona — Cursor ve genel agent’lar için (Claude ile eşlenik)
---

# Workflow: Java Architect

**Kanonik tanım:** [`.claude/agents/java-architect.md`](../../.claude/agents/java-architect.md)  
**İlgili skill:** [`.claude/skills/elly-conventions/SKILL.md`](../../.claude/skills/elly-conventions/SKILL.md)

## Ne zaman kullan

- Yeni entity, servis, controller veya API tasarımı
- Multi-tenant DataSource / `TenantContext` genişletme
- MapStruct, cache, transaction sınırları

## Cursor’da kısa talimat

Şunu söyle veya yapıştır:

> Elly için java-architect rolünde çalış: katman IController → Controller → IService → Service → Repository → Entity; `RootEntityResponse<T>`; constructor injection; MapStruct; tenant izolasyonu. Önce mevcut pattern’i oku, sonra öner.

## API dokümantasyonu

Auth veya endpoint sözleşmesi değişirse `docs/NEXTJS_API_GUIDE.md` güncellemesini planla.
