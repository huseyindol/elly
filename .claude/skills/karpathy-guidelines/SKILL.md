---
name: karpathy-guidelines
description: LLM kodlama hatalarini azaltan davranissal kurallar. Kod yazarken, review yaparken veya refactor ederken — overengineering'i, gereksiz degisikligi, sessiz varsayimlari onlemek ve dogrulanabilir basari kriteri tanimlamak icin otomatik aktif et.
license: MIT
source: https://github.com/forrestchang/andrej-karpathy-skills
---

# Karpathy Guidelines

Andrej Karpathy'nin [LLM kodlama tuzaklari gozlemlerinden](https://x.com/karpathy/status/2015883857489522876) turetilmis davranissal kurallar. Elly projesindeki diger skill'lerle (`elly-conventions`, `multitenant-routing`, vb.) birlikte uygula; bunlar **nasil davranacagini**, digerleri **ne yapacagini** anlatir.

**Tradeoff:** Bu kurallar hizdan cok dikkati onceler. Trivial gorevler icin (tek satir fix, typo) overhead yaratma.

## 1. Think Before Coding

**Varsayma. Kafan karisiksa saklama. Tradeoff'lari yuzeye cikar.**

Kod yazmadan once:
- Varsayimlarini acik soyle. Emin degilsen sor.
- Birden fazla yorum varsa hepsini sun — sessizce birini secme.
- Daha basit bir yol varsa soyle. Gerektiginde itiraz et.
- Belirsizlik varsa dur. Neyin net olmadigini adlandir. Sor.

## 2. Simplicity First

**Problemi cozen minimum kod. Spekulatif bir sey yok.**

- Istenmeyen ozellik ekleme.
- Tek kullanimlik koda abstraction kurma.
- Istenmeyen "esneklik" / "configurability" koyma.
- Olmayacak senaryolara error handling yazma.
- 200 satir yazdin ama 50 satir yetseydi, yeniden yaz.

Test sorusu: "Senior bir muhendis buna 'overengineered' der mi?" — Evetse basitlestir.

## 3. Surgical Changes

**Sadece gerekeni degistir. Sadece kendi yarattigin kirliligi temizle.**

Mevcut kodu duzenlerken:
- Cevredeki kod / yorum / formatlamayi "iyilestirmeye" calisma.
- Bozuk olmayan seyi refactor etme.
- Mevcut stili bozma, kendi tarzini dayatma.
- Ilgisiz dead code gorursen **bahset, silme.**

Degisikliklerin orphan yaratirsa:
- Senin degisikligin yuzunden kullanilmayan import/variable/function'lari kaldir.
- Onceden var olan dead code'u talep olmadan silme.

Test: Degisen her satir dogrudan kullanicinin talebine baglanmali.

## 4. Goal-Driven Execution

**Basari kriterini tanimla. Dogrulanana kadar loop et.**

Gorevleri dogrulanabilir hedeflere cevir:
- "Validation ekle" → "Invalid input icin test yaz, sonra gecsin"
- "Bug'i fix'le" → "Bug'i reproduce eden test yaz, sonra gecsin"
- "X'i refactor et" → "Oncesinde ve sonrasinda testler gecsin"

Cok adimli gorevlerde kisa bir plan sun:
```
1. [Adim] → dogrulama: [check]
2. [Adim] → dogrulama: [check]
3. [Adim] → dogrulama: [check]
```

Guclu basari kriteri bagimsiz loop'a izin verir. Zayif kriter ("calissin iste") surekli clarification gerektirir.

---

**Calisiyor olmasinin isareti:** diff'lerde gereksiz degisiklik azalir, overengineering yuzunden rewrite azalir, clarifying sorular hatalardan *once* gelir.
