# Website Projesi — Blog API Entegrasyonu

**Hedef:** `docs/mockdata/blog` altındaki MDX dosyalarını okuyan `getPostBySlug` /
`getAllPosts` fonksiyonlarını Elly CMS API'dan okuyacak şekilde değiştir.

**Stack:** Next.js App Router, TypeScript, `unstable_cache`, MDX renderer.

---

## API Bilgileri

- **Base URL:** `process.env.NEXT_PUBLIC_API` (`.env.local`'da mevcut)
- **Public endpoint pattern:** `GET /api/v1/public/{tenantId}/posts/...`
  - `tenantId` = `process.env.NEXT_PUBLIC_TENANT_ID` (`.env.local`'a ekle, örn. `tenant1`)
- **Kimlik doğrulama gerekmez** — `PublicApiFilter` token olmadan `posts:read` yetkisi veriyor
- **Response wrapper:** `{ result: boolean, message?: string, data: T }`

---

## 1. `.env.local`'a Ekle

```env
NEXT_PUBLIC_TENANT_ID=tenant1
```

---

## 2. Tip Tanımları

`src/types/blog.ts` dosyasını oluştur veya güncelle:

```typescript
export interface BlogFrontmatter {
  title: string
  description: string | null
  category: string | null
  coverImage: string | null
  publishedAt: string | null   // ISO date string
  author: string | null
  readingTime: string | null
  order: number
}

export interface BlogPost {
  id: number
  slug: string
  content: string              // MDX string (getPostBySlug'dan gelir, getAllPosts'ta boş)
  status: boolean
  frontmatter: BlogFrontmatter
}
```

---

## 3. `src/lib/blog.ts` — API Fonksiyonları

Mevcut `getAllPosts` / `getPostBySlug` implementasyonunu tamamen aşağıdakiyle değiştir:

```typescript
import { unstable_cache } from 'next/cache'

const API_BASE = process.env.NEXT_PUBLIC_API
const TENANT_ID = process.env.NEXT_PUBLIC_TENANT_ID ?? 'tenant1'

interface CmsPost {
  id: number
  title: string
  content: string
  slug: string
  status: boolean
  orderIndex: number | null
  description: string | null
  category: string | null
  coverImage: string | null
  publishedAt: string | null
  author: string | null
  readingTime: string | null
  seoInfo: {
    title: string | null
    description: string | null
    keywords: string | null
  } | null
}

interface ApiResponse<T> {
  result: boolean
  message?: string
  data: T
}

async function fetchAllPosts(): Promise<CmsPost[]> {
  const res = await fetch(
    `${API_BASE}/api/v1/public/${TENANT_ID}/posts/list/summary`,
    { next: { tags: ['blog-posts'] } },
  )
  if (!res.ok) throw new Error(`Posts fetch failed: ${res.status}`)
  const json: ApiResponse<CmsPost[]> = await res.json()
  if (!json.result) throw new Error(json.message ?? 'API error')
  return json.data
}

async function fetchPostBySlug(slug: string): Promise<CmsPost | null> {
  const res = await fetch(
    `${API_BASE}/api/v1/public/${TENANT_ID}/posts/slug/${slug}`,
    { next: { tags: ['blog-posts', `blog-post-${slug}`] } },
  )
  if (res.status === 404) return null
  if (!res.ok) throw new Error(`Post fetch failed: ${res.status}`)
  const json: ApiResponse<CmsPost> = await res.json()
  if (!json.result) return null
  return json.data
}

function toBlogPost(post: CmsPost): BlogPost {
  return {
    id: post.id,
    slug: post.slug,
    content: post.content ?? '',
    status: post.status ?? false,
    frontmatter: {
      title: post.title,
      description: post.description ?? null,
      category: post.category ?? null,
      coverImage: post.coverImage ?? null,
      publishedAt: post.publishedAt ?? null,
      author: post.author ?? null,
      readingTime: post.readingTime ?? null,
      order: post.orderIndex ?? 0,
    },
  }
}

export const getAllPosts = unstable_cache(
  async (): Promise<BlogPost[]> => {
    const posts = await fetchAllPosts()
    return posts
      .filter((p) => p.status)
      .map(toBlogPost)
      .sort((a, b) => {
        if (a.frontmatter.order !== b.frontmatter.order)
          return a.frontmatter.order - b.frontmatter.order
        const dateA = a.frontmatter.publishedAt
          ? new Date(a.frontmatter.publishedAt).getTime()
          : 0
        const dateB = b.frontmatter.publishedAt
          ? new Date(b.frontmatter.publishedAt).getTime()
          : 0
        return dateB - dateA
      })
  },
  ['blog-posts'],
  { revalidate: 86400, tags: ['blog-posts'] },
)

export const getPostBySlug = unstable_cache(
  async (slug: string): Promise<BlogPost | null> => {
    const post = await fetchPostBySlug(slug)
    if (!post) return null
    return toBlogPost(post)
  },
  ['blog-post'],
  { revalidate: 86400, tags: ['blog-posts'] },
)
```

---

## 4. Mevcut Kullanım Yerleri — Değiştirilmeyecek

Aşağıdakiler **aynı kalır**, sadece dönen veri yapısı `BlogFrontmatter` tipine uygun olduğu sürece:

- `MdxContent` bileşeni — `post.content` MDX string'ini render etmeye devam eder
- `ArticleJsonLd` — `post.frontmatter.publishedAt` ISO string olarak gelir
- `generateStaticParams` — `getAllPosts()` çağrısından slug listesi alınır
- Blog listing sayfası — `getAllPosts()` listesi filtre/sıralama değişmeden çalışır

---

## 5. Doğrulama Kriterleri

1. Blog listeleme sayfası açılıyor, yazılar API'dan geliyor
2. Slug tabanlı sayfa (`/blog/[slug]`) doğru postu açıyor
3. `MdxContent` içeriği render ediyor (MDX syntax bozulmuyor)
4. `getPostBySlug` var olmayan slug için `null` dönüyor → 404 sayfası gösteriliyor
5. TypeScript derleme hatası yok
6. `bun run build` başarılı

---

## Notlar

- `getAllPosts` → `/list/summary` endpoint'i kullanır; `content` alanı **gelmez** (listing için yeterli).
- `getPostBySlug` → tam post döner, `content` dahil.
- Cache revalidation: `bun run build` veya `revalidateTag('blog-posts')` ile tetiklenir.
- Eski `gray-matter` ve `fs` importları kaldırılabilir; artık kullanılmayacak.
