# Elly CMS — Agent ve AI Araçları Rehberi

Bu dosya **Cursor**, **Claude Code** ve diğer kod asistanları için ortak giriş noktasıdır. Proje özeti ve komutlar için önce [`CLAUDE.md`](./CLAUDE.md) ve [`.cursor/rules/project-context.mdc`](./.cursor/rules/project-context.mdc) kullanın.

## Dizin yapısı (tek kaynak prensibi)

| Dizin | Amaç | Kim kullanır |
|-------|------|----------------|
| [`.claude/agents/`](./.claude/agents/) | Agent tanımları (YAML frontmatter + rol metni) — **kanonik** | Claude Code Agent Teams |
| [`.claude/skills/`](./.claude/skills/) | Skill paketleri (`SKILL.md`) — **kanonik** | Claude Code Skills |
| [`.claude/commands/`](./.claude/commands/) | Slash / özel komut prompt’ları — **kanonik** | Claude Code |
| [`.claude/agent-memory/`](./.claude/agent-memory/) | Oturumlar arası notlar (changelog, teknik özetler) | Lead + takım |
| [`.claude/settings.json`](./.claude/settings.json) | Claude Code ortamı (örn. `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS`) | Claude Code |
| [`.agents/workflows/`](./.agents/workflows/) | Cursor ve genel agent’lar için **kısa iş akışları** (`.claude` ile aynı roller, taşınabilir metin) | Cursor, herhangi bir agent |
| [`.agents/commands/`](./.agents/commands/) | Komut indeksi (dosyalar `.claude/commands` içinde kalır) | Cursor |

**Özet:** Uzun ve güncel agent metni her zaman `.claude/agents/*.md` içindedir. `.agents/workflows/*.md` aynı rolleri özetler ve Cursor’da “şu persona ile çalış” dediğinde yapıştırılabilir talimat verir.

## Agent listesi (roller)

Tanım dosyası: `.claude/agents/<isim>.md`

| Agent | Dosya | Kısa rol |
|-------|--------|----------|
| `team-lead` | [`team-lead.md`](./.claude/agents/team-lead.md) | Orchestrator, görev dağıtımı, birleştirme |
| `java-architect` | [`java-architect.md`](./.claude/agents/java-architect.md) | Spring / JPA / multi-tenant mimari |
| `code-reviewer` | [`code-reviewer.md`](./.claude/agents/code-reviewer.md) | Pattern, kalite, PR review (çoğunlukla okuma) |
| `devops-engineer` | [`devops-engineer.md`](./.claude/agents/devops-engineer.md) | K8s, Docker, CI/CD, monitoring |
| `security-guard` | [`security-guard.md`](./.claude/agents/security-guard.md) | JWT, OAuth2, tenant izolasyonu (okuma + rapor) |

Cursor’da örnek kullanım: *“`.claude/agents/java-architect.md` rolündeki gibi davran; şu entity tasarımını öner.”*

## Skill listesi

Her biri `SKILL.md` içinde: `.claude/skills/<skill-adı>/SKILL.md`

| Skill | Açıklama (özet) |
|-------|------------------|
| `elly-project-mastery` | Proje durumu, kaldığın yerden devam |
| `elly-conventions` | Paket, katman, DTO, endpoint kuralları |
| `multitenant-routing` | Tenant context, DataSource routing |
| `spring-security-patterns` | JWT, OAuth2, SecurityConfig |
| `dev-session-tracker` | Uzun görevlerde ilerleme notları |

## Slash komutları (Claude Code)

Gerçek dosyalar: `.claude/commands/*.md` — tam metin orada.

| Komut dosyası | Açıklama |
|---------------|----------|
| `cache-audit.md` | Redis cache denetimi |
| `security-review.md` | Dosya/paket güvenlik incelemesi |
| `add-tenant.md` | Yeni tenant için değişiklik listesi |
| `db-migration.md` | SQL migrasyon oluşturma / gözden geçirme |
| `k8s-deploy.md` | K8s durum analizi ve teşhis |
| `new-feature.md` | Elly pattern’ine uygun özellik iskeleti |

İndeks: [`.agents/commands/README.md`](./.agents/commands/README.md)

## İş akışları (Cursor / genel)

`.agents/workflows/` altında persona odaklı kısa rehberler:

- [`lead.md`](./.agents/workflows/lead.md) — Tech lead + Agent Teams koordinasyonu + changelog kuralları
- [`backend.md`](./.agents/workflows/backend.md) — Backend geliştirici akışı
- [`java-architect.md`](./.agents/workflows/java-architect.md)
- [`code-review.md`](./.agents/workflows/code-review.md)
- [`devops.md`](./.agents/workflows/devops.md)
- [`security.md`](./.agents/workflows/security.md)

## Agent hafızası (context persistence)

- Changelog: [`.claude/agent-memory/team-lead/changelog.md`](./.claude/agent-memory/team-lead/changelog.md)
- Konu bazlı notlar: [`.claude/agent-memory/team-lead/`](./.claude/agent-memory/team-lead/)

Orta/büyük değişikliklerde `lead.md` içindeki “Değişiklik Kaydı” bölümünü uygula.

## Cursor Rules ile ilişki

- [`.cursor/rules/lead-team.mdc`](./.cursor/rules/lead-team.mdc) — lead mimari kuralları
- [`.cursor/rules/backend-team.mdc`](./.cursor/rules/backend-team.mdc) — backend kuralları
- [`.cursor/rules/project-context.mdc`](./.cursor/rules/project-context.mdc) — stack ve modül özeti

Bu dosya (`AGENTS.md`) agent rolleri ve dosya konumlarını birleştirir; tekrarlayan uzun metinleri `.cursor/rules` içinde çoğaltma.
