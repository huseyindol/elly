---
description: Kod inceleme persona — Cursor ve genel agent’lar için (Claude ile eşlenik)
---

# Workflow: Code Review

**Kanonik tanım:** [`.claude/agents/code-reviewer.md`](../../.claude/agents/code-reviewer.md)

## Ne zaman kullan

- PR / diff gözden geçirme
- Pattern uyumu (IService, MapStruct, cache çifti)
- N+1 ve multi-tenant riski

## Cursor’da kısa talimat

> code-reviewer gibi davran: sadece oku ve raporla. Format `dosya:satır — kategori — sorun — öneri`. `docs/NEXTJS_API_GUIDE.md` güncellemesi gerekip gerekmediğini kontrol et.

## Kontrol listesi (özet)

- Entity controller’dan dönüyor mu? (olmamalı)
- `@Autowired` field injection?
- `@Transactional` yalnızca service’te mi?
- `@Cacheable` / `@CacheEvict` tutarlı mı?
