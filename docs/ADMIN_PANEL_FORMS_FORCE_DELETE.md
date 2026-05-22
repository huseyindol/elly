# Admin Panel — Form Silme: 409 → Force Delete Modal

**Hedef:** Form listesinde silme butonu tıklandığında önce normal `DELETE /api/v1/forms/{id}`
çağrısı yapılır. `409 Conflict` alınırsa kullanıcıya bağlı kayıtlarla birlikte silme
seçeneği sunan bir uyarı modalı gösterilir.

**Stack:** Next.js 16 App Router, React 19, TypeScript 5.9, Tailwind CSS 4, shadcn/ui, Bun.
API çağrıları `fetcher` utility ile yapılır (token otomatik eklenir).

---

## API Referansı

```
DELETE /api/v1/forms/{id}        → Normal silme (ilişkili kayıt varsa 409 döner)
DELETE /api/v1/forms/{id}/force  → İlişkili tüm submission'larla birlikte sil
```

**Response wrapper:** `{ result: boolean, message?: string, data: T }`

**409 response:**
```json
{
  "result": false,
  "status": 409,
  "error": "Conflict",
  "errorCode": "DATA_INTEGRITY_VIOLATION",
  "message": "Cannot perform operation due to related records"
}
```

**Yetki:** `forms:delete`

---

## 1. Servis Fonksiyonları

Mevcut form servis dosyasına (muhtemelen `src/app/_services/form.services.ts`) ekle:

```typescript
// Normal silme — 409 alırsa hata fırlatır
export const deleteFormService = async (id: number): Promise<void> => {
  const res = await fetcher<BaseResponse<boolean>>(`/api/v1/forms/${id}`, {
    method: 'DELETE',
  })
  if (!res.result) throw new Error(res.message ?? 'Silinemedi')
}

// Submission'larla birlikte zorla sil
export const forceDeleteFormService = async (id: number): Promise<void> => {
  const res = await fetcher<BaseResponse<boolean>>(`/api/v1/forms/${id}/force`, {
    method: 'DELETE',
  })
  if (!res.result) throw new Error(res.message ?? 'Silinemedi')
}
```

`BaseResponse` zaten projede tanımlıysa tekrar tanımlama.

---

## 2. ForceDeleteFormModal Bileşeni

`src/app/_components/forms/ForceDeleteFormModal.tsx` olarak oluştur.
Mevcut Dialog/Modal pattern'ini koru (shadcn/ui `Dialog`).

```typescript
'use client'

import { useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { AlertTriangle } from 'lucide-react'
import { forceDeleteFormService } from '@/app/_services/form.services'

interface Props {
  open: boolean
  formId: number
  formTitle: string
  onClose: () => void
  onDeleted: () => void
}

export function ForceDeleteFormModal({ open, formId, formTitle, onClose, onDeleted }: Props) {
  const [loading, setLoading] = useState(false)

  const handleForceDelete = async () => {
    setLoading(true)
    try {
      await forceDeleteFormService(formId)
      onDeleted()
      onClose()
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <div className="flex items-center gap-3 mb-1">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10 shrink-0">
              <AlertTriangle className="h-5 w-5 text-destructive" />
            </div>
            <DialogTitle className="text-destructive">
              Bu formu silmek istediğinizden emin misiniz?
            </DialogTitle>
          </div>
          <DialogDescription className="space-y-2 pt-2">
            <span className="block font-medium text-foreground">
              &quot;{formTitle}&quot; formu ile bağlantılı kayıtlar mevcut.
            </span>
            <span className="block text-sm">
              Bu işlem aşağıdakileri <strong>kalıcı olarak siler:</strong>
            </span>
            <ul className="list-disc list-inside text-sm space-y-1 ml-1">
              <li>Formun kendisi</li>
              <li>Bu forma ait tüm gönderimler (submissions)</li>
            </ul>
            <span className="block mt-3 rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm font-medium text-destructive">
              ⚠️ Bu işlem geri alınamaz.
            </span>
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={onClose} disabled={loading}>
            İptal
          </Button>
          <Button
            variant="destructive"
            onClick={handleForceDelete}
            disabled={loading}
          >
            {loading ? 'Siliniyor...' : 'Evet, Tümünü Sil'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
```

---

## 3. Form Listesindeki Silme Akışı

Form listesi bileşeninde (muhtemelen `src/app/(baseLayout)/forms/page.tsx` veya
ilgili liste bileşeni) silme butonunun `onClick` handler'ını güncelle:

```typescript
'use client'

import { useState } from 'react'
import { deleteFormService } from '@/app/_services/form.services'
import { ForceDeleteFormModal } from '@/app/_components/forms/ForceDeleteFormModal'
// ... diğer importlar

// State tanımları (mevcut component'a ekle):
const [forceDeleteTarget, setForceDeleteTarget] = useState<{
  id: number
  title: string
} | null>(null)

// Silme handler (mevcut deleteForm/handleDelete fonksiyonunu güncelle):
const handleDelete = async (id: number, title: string) => {
  try {
    await deleteFormService(id)
    // Başarılı silme sonrası listeyi yenile / state'ten kaldır
    setForms((prev) => prev.filter((f) => f.id !== id))
  } catch (e: unknown) {
    // 409 Conflict → force delete modalını göster
    const isConflict =
      e instanceof Error && e.message.toLowerCase().includes('conflict') ||
      (e as { status?: number })?.status === 409
    
    if (isConflict) {
      setForceDeleteTarget({ id, title })
    } else {
      // Diğer hatalar → mevcut toast/notification mekanizması
      console.error(e)
    }
  }
}

// JSX'e modal ekle (return içinde, en dışta):
{forceDeleteTarget && (
  <ForceDeleteFormModal
    open={!!forceDeleteTarget}
    formId={forceDeleteTarget.id}
    formTitle={forceDeleteTarget.title}
    onClose={() => setForceDeleteTarget(null)}
    onDeleted={() => {
      setForms((prev) => prev.filter((f) => f.id !== forceDeleteTarget?.id))
      setForceDeleteTarget(null)
    }}
  />
)}
```

### fetcher 409 kontrolü

`fetcher` utility'si 409'u `throw` olarak mı yoksa response olarak mı dönüyor?
Eğer `throw` ediyorsa yukarıdaki `catch` bloğu çalışır.
Eğer response objesi dönüyorsa şu kontrolü kullan:

```typescript
const res = await fetcher<BaseResponse<boolean>>(`/api/v1/forms/${id}`, { method: 'DELETE' })
if (!res.result) {
  if ((res as unknown as { status: number }).status === 409) {
    setForceDeleteTarget({ id, title })
    return
  }
  throw new Error(res.message)
}
```

Projedeki `fetcher` davranışını kontrol et ve uygun yolu kullan.

---

## 4. Doğrulama Kriterleri

1. Form listesinde silme butonuna basılınca önce normal DELETE çağrısı yapılır
2. İlişkili submission yoksa form direkt silinir (modal çıkmaz)
3. İlişkili submission varsa (409) uyarı modalı açılır
4. Modal: kırmızı uyarı ikonu, form adı, "geri alınamaz" vurgusu görünür
5. "İptal" → modal kapanır, form silinmez
6. "Evet, Tümünü Sil" → loading durumu → force delete → form listeden kalkar
7. `bun dev` veya `bun run build` TypeScript hatası yok
