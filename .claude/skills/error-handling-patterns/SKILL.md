---
name: error-handling-patterns
description: Elly'deki exception hiyerarşisi, GlobalExceptionHandler, ErrorResponse formatı ve yeni exception ekleme pattern'leri. Hata yönetimi, yeni exception tipi veya API error response tasarımında otomatik aktif et.
version: 1.0.0
---

# Elly Error Handling Patterns

## Exception Hiyerarşisi

```
RuntimeException
  └── BaseException (abstract) — status: HttpStatus, errorCode: String
        ├── ResourceNotFoundException   → 404, "RESOURCE_NOT_FOUND"
        ├── BadRequestException         → 400, "BAD_REQUEST"
        ├── ValidationException         → 400, "VALIDATION_ERROR"
        ├── FormValidationException     → 400, field-level errors (Map<fieldId, message>)
        ├── ConflictException           → 409, "CONFLICT"
        ├── ForbiddenException          → 403, "FORBIDDEN"
        └── UnauthorizedException       → 401, "UNAUTHORIZED"
```

## ErrorResponse Formatı

Tüm hata response'ları aynı JSON yapısında döner:

```json
{
  "success": false,
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Post not found with id: 42",
  "path": "/api/v1/posts/42",
  "validationErrors": null
}
```

Validation hatalarında `validationErrors` alanı dolu gelir:
```json
{
  "validationErrors": {
    "title": "Title is required",
    "slug": "Slug must be unique"
  }
}
```

## GlobalExceptionHandler — Yakalanan Exception'lar

| Exception | HTTP Status | Error Code | Ne zaman fırlatılır |
|-----------|-------------|------------|---------------------|
| `BaseException` (alt sınıflar) | exception'a göre | exception'a göre | Service katmanı |
| `AccessDeniedException` | 403 | ACCESS_DENIED | @PreAuthorize başarısız |
| `AuthenticationException` | 401 | BAD_CREDENTIALS / ACCOUNT_DISABLED / ACCOUNT_LOCKED | Login hatası |
| `MethodArgumentNotValidException` | 400 | VALIDATION_FAILED | @Valid annotation |
| `ConstraintViolationException` | 400 | CONSTRAINT_VIOLATION | Jakarta validation |
| `DataIntegrityViolationException` | 409 | DATA_INTEGRITY_VIOLATION | DB unique/FK ihlali |
| `HttpMessageNotReadableException` | 400 | INVALID_REQUEST_BODY | Bozuk JSON body |
| `MethodArgumentTypeMismatchException` | 400 | INVALID_PARAMETER_TYPE | Yanlış parametre tipi |
| `MissingServletRequestParameterException` | 400 | MISSING_PARAMETER | Eksik query param |
| `HttpRequestMethodNotSupportedException` | 405 | METHOD_NOT_ALLOWED | Yanlış HTTP method |
| `NoHandlerFoundException` / `NoResourceFoundException` | 404 | ENDPOINT_NOT_FOUND | Olmayan endpoint |
| `RuntimeException` | 500 | RUNTIME_ERROR | Beklenmeyen hata |
| `Exception` | 500 | INTERNAL_SERVER_ERROR | Catch-all |

## Yeni Exception Ekleme Pattern'i

### 1. Exception sınıfı oluştur
```java
package com.cms.exception;

import org.springframework.http.HttpStatus;

public class XxxException extends BaseException {
    public XxxException(String message) {
        super(message, HttpStatus.XXX, "XXX_ERROR_CODE");
    }
}
```

### 2. Service'ten fırlat
```java
// Service katmanında — asla Controller'da exception fırlatma
throw new ResourceNotFoundException("Post not found with id: " + id);
throw new BadRequestException("Invalid email format");
throw new ConflictException("Slug already exists: " + slug);
```

### 3. Özel handler gerekiyorsa (opsiyonel)
Çoğu durumda `BaseException` handler'ı yeterli. Özel response formatı gerekiyorsa:
```java
// GlobalExceptionHandler'a ekle
@ExceptionHandler(XxxException.class)
public ResponseEntity<ErrorResponse> handleXxxException(XxxException ex, HttpServletRequest request) {
    // Özel loglama veya response oluşturma
}
```

## Kritik Kurallar

1. **Exception sadece Service katmanında fırlatılır** — Controller'da try/catch yapma
2. **BaseException kullan** — raw RuntimeException fırlatma
3. **Anlamlı mesaj yaz** — "Error" değil, "Post not found with id: 42"
4. **ErrorCode string constant** — magic string kullanma, BaseException'ın constructor'ında tanımlı
5. **GlobalExceptionHandler'ı genişlet** — yeni catch-all veya override yapma, mevcut hiyerarşiye ekle
6. **Log seviyesi:** business hata → `log.error`, validation → `log.warn`
