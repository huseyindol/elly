# Mail Smoke Test — Otomatik Dogrulama Playbook

> Mail+Form v2 akisinin uctan uca calistigini tek komut dizisiyle dogrulayan skill.
> Kullanici "mail smoke test yap" / "dummy mail gonder" dediginde bu akisi uygula.

## Ne Zaman Kullan

- Yeni bir deploy sonrasi mail sisteminin calistigini hizlica dogrulamak
- SMTP credentials yenilendi / AES key rotate edildi, test gerekli
- Panel'den form olusturduktan sonra bildirim akisinin duzgun oldugunu gormek
- Kod degisikligi sonrasi regression check

## Nasil Kullan

Kullanici istegi:
> "mail smoke test yap" / "dummy mail gonder" / "mail sistemini dogrula"

Adimlari asagidaki siraya gore UYGULA (curl + kubectl). Her adimin sonucunu bir onceki adimda olusan sonuca baglayarak ilerle (TOKEN, MAIL_ACC_ID, FORM_ID).

---

## Gerekli Girdiler (Kullanicidan Al)

Minimum:
- `API_BASE` (ornek: `https://api.huseyindol.com`)
- `USERNAME`, `PASSWORD`, `TENANT_ID` (admin login icin)
- `RECIPIENT_EMAIL` (maili kimin alacagi — test adresi)

Opsiyonel (yoksa yeni olustur):
- `MAIL_ACCOUNT_ID` — varsa kullan, yoksa olusturma sorusu sor
- `FORM_ID` — varsa kullan, yoksa olustur

Eger hic `smtpUsername`/`smtpPassword`/`fromAddress` yoksa ve `MAIL_ACCOUNT_ID` verilmediyse: mail hesabi olusturulamaz, kullaniciya Gmail App Password rehberi sun, durak noktasi.

---

## Playbook Adimlari

### A. Login (token al)

```bash
TOKEN=$(curl -sS -X POST "$API_BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"usernameOrEmail\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"tenantId\":\"$TENANT_ID\",\"loginType\":\"admin\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
```

Dogrulama: `echo "${TOKEN:0:20}"` ile token geldi mi bak. Bos ise: credentials yanlis, durak.

### B. Mail Hesabi (varsa kullan, yoksa olustur)

```bash
# Mevcut aktif hesaplari sorgula
curl -sS "$API_BASE/api/v1/mail-accounts/active" \
  -H "Authorization: Bearer $TOKEN"
```

Eger response'ta en az bir aktif hesap varsa: ilk hesap `id`'sini `MAIL_ACC_ID` olarak al.

Yoksa (ve kullanici SMTP bilgilerini verdiyse):

```bash
curl -sS -X POST "$API_BASE/api/v1/mail-accounts" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smoke Test Gmail",
    "fromAddress": "'$FROM_ADDRESS'",
    "smtpHost": "smtp.gmail.com",
    "smtpPort": 587,
    "smtpUsername": "'$SMTP_USER'",
    "smtpPassword": "'$SMTP_PASS'",
    "active": true
  }'
```

Sonra verify:

```bash
curl -sS -X POST "$API_BASE/api/v1/mail-accounts/$MAIL_ACC_ID/verify" \
  -H "Authorization: Bearer $TOKEN"
```

Beklenti: `"SMTP baglantisi basarili"`. Degilse: AES decrypt veya Gmail auth sorunu — durak, hata mesajini kullaniciya goster.

### C. Form Definition (varsa kullan, yoksa olustur)

```bash
# Mevcut formlari listele
curl -sS "$API_BASE/api/v1/forms/list/active" \
  -H "Authorization: Bearer $TOKEN"
```

Eger uygun form varsa (notificationEnabled=true, senderMailAccountId eslesen): `FORM_ID` olarak al.

Yoksa olustur:

```bash
curl -sS -X POST "$API_BASE/api/v1/forms" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Smoke Test Form",
    "version": 1,
    "active": true,
    "senderMailAccountId": '$MAIL_ACC_ID',
    "recipientEmail": "'$RECIPIENT_EMAIL'",
    "notificationSubject": "[Smoke Test] '$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "notificationEnabled": true,
    "schema": {
      "config": {"layout": "vertical"},
      "fields": [
        {"id": "name", "type": "text", "label": "Ad Soyad", "required": true},
        {"id": "email", "type": "text", "label": "E-posta", "required": true},
        {"id": "message", "type": "text", "label": "Mesaj", "required": false}
      ],
      "steps": null
    }
  }'
```

### D. Dummy Payload Uret + Submit

Dummy icerigi deterministic degil; her seferinde farkli iceren mail test edilsin.

```bash
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
SMOKE_ID=$(openssl rand -hex 4)  # kisa random id

curl -sS -X POST "$API_BASE/api/v1/forms/$FORM_ID/submit" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"payload\": {
      \"name\": \"Smoke Bot #$SMOKE_ID\",
      \"email\": \"bot-$SMOKE_ID@test.local\",
      \"message\": \"Otomatik smoke test. Zaman: $TIMESTAMP, SmokeID: $SMOKE_ID\"
    }
  }"
```

Response'tan `data.id` (submissionId) al, bir sonraki adimdaki EmailLog kontrolunde kullan.

### E. Dogrulama

EmailLog durumu (kullaniciya ozet):

```bash
# Sunucuda — tenant DB'sine bagli olarak
sudo kubectl exec -n elly postgres-$TENANT_ID-0 -- \
  psql -U postgres -d elly_$TENANT_ID \
  -c "SELECT id, status, recipient, retry_count, sent_at FROM email_logs ORDER BY id DESC LIMIT 3;"
```

Expected: en yeni satirda `status=SENT`, `sent_at` dolu, `retry_count=0`.

Eger `PENDING`/`FAILED` ise:

```bash
sudo kubectl logs -n elly deployment/elly-app --since=3m | \
  grep -iE "Mail isleniyor|Mail gonderildi|Mail gonderilemedi" | tail -10
```

Log'da hata var mi? Buldugunu kullaniciya aktar ve yorumla.

---

## Tipik Hata Senaryolari

| Hata Mesaji | Kok Neden | Cozum |
|---|---|---|
| 401 login'de | Yanlis credential | Username/password/tenantId dogru mu? |
| `Invalid AES key length` | AES_SECRET_KEY 32 ASCII degil | Secret'i kontrol et (`kubectl exec ... echo $AES_SECRET_KEY \| wc -c`) |
| `Authentication failed` (SMTP) | Gmail App Password yanlis/expired | Yeni app password olustur, `PUT /mail-accounts/{id}` ile guncelle |
| `Connection timed out` (SMTP) | Network/firewall smtp.gmail.com:587 engelli | Cluster'dan disa network kontrol |
| `Could not initialize proxy [MailAccount]` | **v2 eski kodda fix'lendi (aa943ef)** — `findByIdWithMailAccount` kullanilmiyor | Deploy ettin mi kontrol et |
| EmailLog PENDING kaliyor (5dk+) | RabbitMQ consumer down / queue islemiyor | `kubectl get pods` + RabbitMQ management UI kontrol |
| EmailLog yaratilmiyor | Form.notificationEnabled=false veya senderMailAccount null | Form ayarlarini kontrol et |

---

## Kullaniciya Ozet Cikti Sablonu

Smoke test bittiginde kullaniciya su formatta sunucu durumu ozetle:

```
Mail Smoke Test - <zaman>

1. Login                  ✅ / ❌
2. Mail Account           ✅ (id=N, kullanildi/olusturuldu)
3. SMTP Verify            ✅ "SMTP baglantisi basarili"
4. Form Definition        ✅ (id=N, kullanildi/olusturuldu)
5. Form Submit            ✅ (submissionId=N)
6. EmailLog               ✅ id=N SENT (sent_at=... gecikme=X sn)
7. Gmail Inbox            ⏳ Kullanici kontrol edecek

Sonuc: BASARILI / BASARISIZ
```

Basarisiz ise son 3 adimdan hangisinde takildigini ve log'dan cikan somut hata mesajini ekle.

---

## Guvenlik Notu

Dummy mail icerikleri test verisi olsa da gercek adresiz mail gonderimi yapildigindan:
- `RECIPIENT_EMAIL` her zaman kontrol altinda tutulan bir adres olmali (kullaniciya sor)
- Production'da smoke test yapiliyorsa uyari ver: "Bu gercek SMTP hesabindan mail gonderilecek, Gmail kota tuketir"
- Gun icinde 10+ smoke test yapma, Gmail daily limit 500 mail

## Gelismeler (v3+)

- **v3 retry endpoint:** `POST /api/v1/emails/{id}/retry` — FAILED kayitlari tek komutla reset + re-queue eder. Skill'e entegre edilebilir (E adiminda FAILED gorulurse otomatik retry dene).
- **v4 template service:** Template'ler panel'de host edilince, smoke test payload'inda `templateKey` secimi gerekebilir.
