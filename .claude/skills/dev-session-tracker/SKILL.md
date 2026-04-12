---
name: dev-session-tracker
description: Uzun suren gelistirme sureclerini takip eder. Yeni oturum basladiginda veya mevcut bir goreve devam edileceginde otomatik aktif et. Yapilan isleri, kalinan noktayi ve sonraki adimlari kaydeder.
version: 2.0.0
---

# Geliştirme Oturumu Takipçi (Dev Session Tracker)

Uzun süren geliştirme görevlerinde ilerlemeyi izle, kalınan yeri kaydet ve sonraki oturumlarda kaldığı yerden devam etmeyi sağla.

## Ne Zaman Kullan

- Birden fazla oturuma yayılabilecek büyük özellik geliştirmelerinde
- Karmaşık bug fix süreçlerinde
- Çok katmanlı refactoring işlerinde
- Kullanıcı "devam et", "kaldığı yerden", "nerede kalmıştık" dediğinde
- Her yeni oturum başlangıcında (otomatik)

## Oturum Başlatma — Zorunlu Adımlar

```
1. .claude/agent-memory/team-lead/changelog.md → Son yapılanları oku
2. .claude/agent-memory/team-lead/*.md         → Aktif görev dosyalarını oku
3. git log --oneline -5                        → Son commit'leri kontrol et
4. git status                                  → Uncommitted değişiklikler var mı?
5. elly-project-mastery skill'ini aktif et     → Tam proje bağlamı için
```

Sonra kullanıcıya şunu söyle:
> "Son oturumda [X yapıldı]. [Aktif görev varsa: Devam eden görev: Y]. Nereden devam edelim?"

## Görev Dosyası Formatı

Her büyük görev için `.claude/agent-memory/team-lead/` altında bir `.md` dosyası oluştur:

```markdown
# [Görev Adı] — Teknik Detay

## Tarih: YYYY-MM-DD
## Durum: [Başlamadı | Devam Ediyor | Beklemede | Tamamlandı]

## Hedef
- Ne yapılacak, neden yapılacak

## Tamamlanan Adımlar
- [x] Adım 1 — açıklama
- [x] Adım 2 — açıklama

## Sıradaki Adımlar
- [ ] Adım 3 — açıklama
- [ ] Adım 4 — açıklama

## Değişen/Değişecek Dosyalar
- dosya1.java — açıklama
- dosya2.java — açıklama

## Mimari Kararlar / Notlar
- Önemli mimari karar veya blocker bilgisi

## Breaking Changes
- Varsa listele
```

## Oturum Devam Ettirme

Kullanıcı mevcut bir göreve devam etmek istediğinde:

1. `.claude/agent-memory/team-lead/` altındaki ilgili görev dosyasını oku
2. `changelog.md`'den son durumu kontrol et
3. Git log'dan son commit'leri incele
4. "Sıradaki Adımlar" bölümünden kaldığı yerden devam et
5. Her tamamlanan adımı görev dosyasında güncelle

## Oturum Bitirme

Görev tamamlandığında veya oturum sonlanandığında:

1. Görev dosyasındaki durumu güncelle (tamamlanan adımlar, sıradaki adımlar)
2. `changelog.md`'ye özet ekle (eğer orta/büyük değişiklik yapıldıysa)
3. `elly-project-mastery` skill'ini güncelle (yeni kritik bilgi varsa)
4. Kullanıcıya özet sun: "Bugün X yapıldı. Sonraki adım: Y"

## Changelog Güncelleme Kuralları

Aşağıdaki durumlardan herhangi birinde `changelog.md` güncellenmelidir:
1. Yeni entity / tablo eklenmesi
2. Yeni servis / controller eklenmesi
3. Güvenlik yapılandırmasında değişiklik
4. Mimari değişiklik (cache, filter, interceptor, vb.)
5. API endpoint ekleme/silme/değişiklik
6. Performans optimizasyonu
7. 3+ dosyayı etkileyen herhangi bir geliştirme

## Örnek Kullanım

```
Kullanıcı: "Mail özelliğine devam edelim"

Claude:
1. tenant-mail-smtp.md oku → durum TAMAMLANDI
2. changelog.md oku → son yapılanları gör
3. git log ile son commit'leri kontrol et
4. Kullanıcıya: "Mail özelliği tamamlandı (2026-04-12).
   Yeni görev mi başlıyoruz, yoksa mail sisteminde iyileştirme mi?"
```

```
Kullanıcı: "Yeni bir özellik ekleyelim"

Claude:
1. changelog.md oku → mevcut durumu anla
2. elly-project-mastery skill'ini aktif et → mimari bağlam
3. elly-conventions skill'ini aktif et → kod yazma kuralları
4. Yeni görev dosyası oluştur
5. Geliştirmeye başla
```
