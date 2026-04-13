# `.agents` — Cursor ve genel AI agent iş akışları

Bu klasör **Claude Code** ile aynı takım rollerini paylaşır; uzun ve güncel tanımlar **her zaman** üst dizindeki [`.claude/agents/`](../.claude/agents/) içindedir.

## Ne nerede?

| İçerik | Konum |
|--------|--------|
| Proje + agent girişi | Repo kökü [`AGENTS.md`](../AGENTS.md) |
| Claude özeti | [`CLAUDE.md`](../CLAUDE.md) |
| Kanonik agent tanımları | [`.claude/agents/`](../.claude/agents/) |
| Skills | [`.claude/skills/`](../.claude/skills/) |
| Slash komutları | [`.claude/commands/`](../.claude/commands/) — indeks: [`commands/README.md`](./commands/README.md) |
| Changelog / hafıza | [`.claude/agent-memory/`](../.claude/agent-memory/) |

## Workflows (`workflows/`)

Kısa, yapıştırılabilir iş akışları — detay için ilgili `.claude/agents/*.md` dosyasını aç.

- `lead.md` — orchestration + zorunlu changelog
- `backend.md` — backend geliştirici
- `java-architect.md` — mimari / Spring
- `code-review.md` — kod incelemesi
- `devops.md` — K8s / Docker / CI
- `security.md` — güvenlik analizi

## Hafıza (`memory/`)

Changelog ve konu notlarının **tek** yeri: [`.claude/agent-memory/`](../.claude/agent-memory/) — bkz. [`memory/README.md`](./memory/README.md).
