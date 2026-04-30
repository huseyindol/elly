# Elly CMS — Agent ve AI Araclari Rehberi

Bu dosya **Cursor**, **Claude Code** ve diger kod asistanlari icin ortak giris noktasidir. Proje ozeti ve komutlar icin once [`CLAUDE.md`](./CLAUDE.md) ve [`.cursor/rules/project-context.mdc`](./.cursor/rules/project-context.mdc) kullanin.

## Karar Agaci — Ne Zaman Ne Kullan?

```
Gorev geldi
  |
  +-- Tek dosya / kucuk degisiklik? → Kendin yap, skill'leri referans al
  |
  +-- Yeni entity/ozellik? → /new-feature komutu veya java-architect agent
  |     Kullanilacak skill'ler: elly-conventions, multitenant-routing,
  |     redis-cache-patterns, error-handling-patterns
  |
  +-- Cache sorunu / optimizasyon? → /cache-audit komutu
  |     Kullanilacak skill: redis-cache-patterns
  |
  +-- Guvenlik incelemesi? → /security-review komutu veya security-guard agent
  |     Kullanilacak skill'ler: spring-security-patterns, multitenant-routing
  |
  +-- K8s / deployment sorunu? → /k8s-deploy komutu veya devops-engineer agent
  |
  +-- Yeni tenant ekleme? → /add-tenant komutu
  |     Kullanilacak skill: multitenant-routing
  |
  +-- DB migration? → /db-migration komutu
  |
  +-- RabbitMQ yeni queue / consumer? → java-architect agent
  |     Kullanilacak skill: rabbitmq-patterns
  |
  +-- Buyuk ozellik (3+ dosya)? → team-lead ile Agent Teams
  |     team-lead gorev dagitir, skill'leri teammate'lere yonlendirir
  |
  +-- Kod review? → code-reviewer agent
  |     Kullanilacak skill'ler: elly-conventions, redis-cache-patterns,
  |     error-handling-patterns
  |
  +-- Oturum devam / nerede kaldik? → dev-session-tracker + elly-project-mastery
```

## Dizin yapisi (tek kaynak prensibi)

| Dizin | Amac | Kim kullanir |
|-------|------|----------------|
| [`.claude/agents/`](./.claude/agents/) | Agent tanimlari (YAML frontmatter + rol metni) — **kanonik** | Claude Code Agent Teams |
| [`.claude/skills/`](./.claude/skills/) | Skill paketleri (`SKILL.md`) — **kanonik** | Claude Code Skills + Agent'lar |
| [`.claude/commands/`](./.claude/commands/) | Slash / ozel komut prompt'lari — **kanonik** | Claude Code |
| [`.claude/agent-memory/`](./.claude/agent-memory/) | Oturumlar arasi notlar (changelog, teknik ozetler) | Lead + takim |
| [`.claude/settings.json`](./.claude/settings.json) | Claude Code ortami (orn. `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS`) | Claude Code |
| [`.agents/workflows/`](./.agents/workflows/) | Cursor ve genel agent'lar icin **kisa is akislari** | Cursor, herhangi bir agent |
| [`.agents/commands/`](./.agents/commands/) | Komut indeksi (dosyalar `.claude/commands` icinde kalir) | Cursor |

## Agent listesi (roller)

Tanim dosyasi: `.claude/agents/<isim>.md`

| Agent | Dosya | Kisa rol | Ilgili Skill'ler |
|-------|--------|----------|------------------|
| `team-lead` | [`team-lead.md`](./.claude/agents/team-lead.md) | Orchestrator, gorev dagitimi | **karpathy-guidelines**, elly-project-mastery, dev-session-tracker |
| `java-architect` | [`java-architect.md`](./.claude/agents/java-architect.md) | Spring / JPA / multi-tenant mimari | **karpathy-guidelines**, elly-conventions, multitenant-routing, redis-cache-patterns, rabbitmq-patterns, error-handling-patterns, spring-security-patterns |
| `code-reviewer` | [`code-reviewer.md`](./.claude/agents/code-reviewer.md) | Pattern, kalite, PR review | **karpathy-guidelines**, elly-conventions, redis-cache-patterns, error-handling-patterns, multitenant-routing |
| `devops-engineer` | [`devops-engineer.md`](./.claude/agents/devops-engineer.md) | K8s, Docker, CI/CD, monitoring | **karpathy-guidelines**, elly-project-mastery, rabbitmq-patterns |
| `security-guard` | [`security-guard.md`](./.claude/agents/security-guard.md) | JWT, OAuth2, tenant izolasyonu | **karpathy-guidelines**, spring-security-patterns, multitenant-routing, error-handling-patterns |

Cursor'da ornek kullanim: *"`.claude/agents/java-architect.md` rolundeki gibi davran; su entity tasarimini oner."*

## Skill listesi

Her biri `SKILL.md` icinde: `.claude/skills/<skill-adi>/SKILL.md`

| Skill | Aciklama | Otomatik Tetikleme |
|-------|----------|---------------------|
| `elly-project-mastery` | Proje durumu, kaldigi yerden devam | Her oturum basinda |
| `elly-conventions` | Paket, katman, DTO, endpoint kurallari | Yeni sinif/endpoint olustururken |
| `multitenant-routing` | Tenant context, DataSource routing | Tenant islemleri, cross-tenant query |
| `spring-security-patterns` | JWT, OAuth2, SecurityConfig | Auth flow, endpoint guvenligi |
| `redis-cache-patterns` | Cache key, TTL, invalidation, fallback | Cache ekleme/sorun giderme |
| `rabbitmq-patterns` | Queue/exchange, consumer, retry/DLQ | Yeni queue, consumer, mesaj akisi |
| `error-handling-patterns` | BaseException, GlobalExceptionHandler | Yeni exception, error response |
| `dev-session-tracker` | Uzun gorevlerde ilerleme notlari | "Devam et", "nerede kaldik" |
| `karpathy-guidelines` | LLM kodlama tuzaklarini azaltan davranissal kurallar (think-before-coding, simplicity, surgical, goal-driven) | Her kod yazma/review/refactor gorevinde |

### Skill -> Agent Besleme Haritasi

```
karpathy-guidelines ──── ★ tum agent'lar (cross-cutting davranissal)
elly-conventions ──────── java-architect, code-reviewer
multitenant-routing ───── java-architect, security-guard, code-reviewer
spring-security-patterns  security-guard, java-architect
redis-cache-patterns ──── java-architect, code-reviewer
rabbitmq-patterns ─────── java-architect, devops-engineer
error-handling-patterns ── java-architect, code-reviewer, security-guard
elly-project-mastery ──── team-lead, devops-engineer
dev-session-tracker ───── team-lead
```

## Slash komutlari (Claude Code)

Gercek dosyalar: `.claude/commands/*.md` — tam metin orada.

| Komut dosyasi | Aciklama | Ilgili Skill |
|---------------|----------|--------------|
| `new-feature.md` | Elly pattern'ine uygun ozellik iskeleti | elly-conventions |
| `cache-audit.md` | Redis cache denetimi | redis-cache-patterns |
| `security-review.md` | Dosya/paket guvenlik incelemesi | spring-security-patterns |
| `add-tenant.md` | Yeni tenant icin degisiklik listesi | multitenant-routing |
| `db-migration.md` | SQL migrasyon olusturma / gozden gecirme | — |
| `k8s-deploy.md` | K8s durum analizi ve teshis | — |

Indeks: [`.agents/commands/README.md`](./.agents/commands/README.md)

## Is akislari (Cursor / genel)

`.agents/workflows/` altinda persona odakli kisa rehberler:

- [`lead.md`](./.agents/workflows/lead.md) — Tech lead + Agent Teams koordinasyonu + changelog kurallari
- [`backend.md`](./.agents/workflows/backend.md) — Backend gelistirici akisi
- [`java-architect.md`](./.agents/workflows/java-architect.md)
- [`code-review.md`](./.agents/workflows/code-review.md)
- [`devops.md`](./.agents/workflows/devops.md)
- [`security.md`](./.agents/workflows/security.md)

## Agent hafizasi (context persistence)

- Changelog: [`.claude/agent-memory/team-lead/changelog.md`](./.claude/agent-memory/team-lead/changelog.md)
- Konu bazli notlar: [`.claude/agent-memory/team-lead/`](./.claude/agent-memory/team-lead/)

Orta/buyuk degisikliklerde `lead.md` icindeki "Degisiklik Kaydi" bolumunu uygula.

## Cursor Rules ile iliski

- [`.cursor/rules/lead-team.mdc`](./.cursor/rules/lead-team.mdc) — lead mimari kurallari
- [`.cursor/rules/backend-team.mdc`](./.cursor/rules/backend-team.mdc) — backend kurallari
- [`.cursor/rules/project-context.mdc`](./.cursor/rules/project-context.mdc) — stack ve modul ozeti
- [`.cursor/rules/karpathy-guidelines.mdc`](./.cursor/rules/karpathy-guidelines.mdc) — davranissal kurallar (alwaysApply)

Bu dosya (`AGENTS.md`) agent rolleri, skill'ler ve dosya konumlarini birlestirir; tekrarlayan uzun metinleri `.cursor/rules` icinde cogaltma.
