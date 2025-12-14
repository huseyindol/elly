# Global Exception Handler Dokümantasyonu

## 📋 İçindekiler
- [Genel Bakış](#genel-bakış)
- [Exception Yapısı](#exception-yapısı)
- [Kullanılabilir Exception Sınıfları](#kullanılabilir-exception-sınıfları)
- [Kullanım Örnekleri](#kullanım-örnekleri)
- [Error Response Formatı](#error-response-formatı)
- [Otomatik Olarak Yakalanan Exception'lar](#otomatik-olarak-yakalanan-exceptionlar)

## Genel Bakış

Bu proje, Spring Boot 3.5.7 için kapsamlı bir global exception handling mekanizması içermektedir. Tüm hatalar merkezi bir yerden yönetilir ve tutarlı bir JSON formatında döndürülür.

### Özellikler
- ✅ Merkezi exception yönetimi
- ✅ Tutarlı error response formatı
- ✅ Otomatik logging (SLF4J)
- ✅ HTTP status code yönetimi
- ✅ Validation hatalarının detaylı raporlanması
- ✅ Database constraint violation handling
- ✅ Custom exception sınıfları

## Exception Yapısı

### BaseException
Tüm custom exception'ların extend ettiği base sınıf:

```java
public abstract class BaseException extends RuntimeException {
  private final HttpStatus status;
  private final String errorCode;
  
  // Constructor methods...
}
```

## Kullanılabilir Exception Sınıfları

### 1. ResourceNotFoundException (404)
Bir kaynak bulunamadığında kullanılır.

**Constructors:**
```java
// Basit mesaj
new ResourceNotFoundException("Resource not found")

// ID ile
new ResourceNotFoundException("Component", 123L)
// Sonuç: "Component with id 123 not found"

// Alan adı ve değer ile
new ResourceNotFoundException("Page", "slug", "home-page")
// Sonuç: "Page not found with slug: 'home-page'"
```

**Örnek Kullanım:**
```java
@Service
public class PageService {
  public Page getPageById(Long id) {
    return pageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Page", id));
  }
  
  public Page getPageBySlug(String slug) {
    return pageRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Page", "slug", slug));
  }
}
```

### 2. ValidationException (400)
İş kuralı validasyonları için kullanılır.

**Constructor:**
```java
new ValidationException("BANNER tipindeki component'e widget eklenemez")
```

**Örnek Kullanım:**
```java
@Service
public class ComponentService {
  public Component saveComponent(Component component, List<Long> widgetIds) {
    if (component.getType() == ComponentTypeEnum.BANNER 
        && widgetIds != null && !widgetIds.isEmpty()) {
      throw new ValidationException("BANNER tipindeki component'e widget eklenemez");
    }
    // ...
  }
}
```

### 3. BadRequestException (400)
Geçersiz request'ler için kullanılır.

**Constructor:**
```java
new BadRequestException("Invalid request parameters")
new BadRequestException("File processing failed", cause)
```

**Örnek Kullanım:**
```java
@Service
public class FileService {
  public String saveImage(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File is empty or null");
    }
    
    if (!isImageFile(file)) {
      throw new BadRequestException("File is not an image");
    }
    // ...
  }
}
```

### 4. ConflictException (409)
Kaynak çakışmaları için kullanılır (örn: duplicate entry).

**Constructor:**
```java
new ConflictException("A user with this email already exists")
new ConflictException("Resource conflict occurred", cause)
```

**Örnek Kullanım:**
```java
@Service
public class UserService {
  public User createUser(User user) {
    if (userRepository.existsByEmail(user.getEmail())) {
      throw new ConflictException("A user with this email already exists");
    }
    return userRepository.save(user);
  }
}
```

## Error Response Formatı

Tüm hatalar aşağıdaki JSON formatında döner:

### Basit Hata Response
```json
{
  "result": false,
  "timestamp": "2024-12-14T10:30:45.123",
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Component with id 123 not found",
  "path": "/api/v1/components/123"
}
```

### Validation Hataları
```json
{
  "result": false,
  "timestamp": "2024-12-14T10:30:45.123",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/components",
  "validationErrors": {
    "name": "must not be blank",
    "type": "must not be null",
    "orderIndex": "must be greater than or equal to 0"
  }
}
```

### Detaylı Hata Response
```json
{
  "result": false,
  "timestamp": "2024-12-14T10:30:45.123",
  "status": 500,
  "error": "Internal Server Error",
  "errorCode": "RUNTIME_ERROR",
  "message": "An unexpected error occurred",
  "path": "/api/v1/components",
  "details": [
    "Database connection lost",
    "Retry failed after 3 attempts"
  ]
}
```

## Otomatik Olarak Yakalanan Exception'lar

Global Exception Handler aşağıdaki exception'ları otomatik olarak yakalar ve işler:

### 1. Validation Exceptions
- `MethodArgumentNotValidException` - @Valid annotation hataları
- `ConstraintViolationException` - Constraint violation hataları

**Örnek:** Entity'de @NotNull, @NotBlank, @Size gibi validasyonlar

```java
public class DtoComponentIU {
  @NotBlank(message = "Name is required")
  private String name;
  
  @NotNull(message = "Type is required")
  private ComponentTypeEnum type;
  
  @Min(value = 0, message = "Order index must be positive")
  private Integer orderIndex;
}
```

### 2. Database Exceptions
- `DataIntegrityViolationException` - Veritabanı constraint hataları
  - Unique constraint violations
  - Foreign key violations
  - Not null violations

**Response Örneği:**
```json
{
  "status": 409,
  "error": "Conflict",
  "errorCode": "DATA_INTEGRITY_VIOLATION",
  "message": "A record with this value already exists"
}
```

### 3. HTTP Request Exceptions
- `HttpMessageNotReadableException` - Geçersiz JSON formatı
- `MethodArgumentTypeMismatchException` - Yanlış parametre tipi
- `MissingServletRequestParameterException` - Eksik parametre
- `HttpRequestMethodNotSupportedException` - Desteklenmeyen HTTP method

**Örnek:**
```bash
# GET yerine POST kullanıldığında
GET /api/v1/components/123
```

```json
{
  "status": 405,
  "error": "Method Not Allowed",
  "errorCode": "METHOD_NOT_ALLOWED",
  "message": "Request method 'GET' not supported. Supported methods: POST, PUT"
}
```

### 4. 404 Errors
- `NoHandlerFoundException` - Endpoint bulunamadı
- `NoResourceFoundException` - Resource bulunamadı

### 5. Generic Exceptions
- `RuntimeException` - Genel runtime hataları
- `Exception` - Diğer tüm beklenmeyen hatalar

## Controller'da Kullanım

Controller'larda exception handling'e gerek yoktur. Service layer'dan fırlatılan exception'lar otomatik olarak yakalanır:

```java
@RestController
@RequestMapping("/api/v1/components")
public class ComponentController {

  @Autowired
  private IComponentService componentService;

  @GetMapping("/{id}")
  public RootEntityResponse<DtoComponent> getComponentById(@PathVariable Long id) {
    // Exception handling'e gerek yok!
    // Service'den fırlatılan exception otomatik yakalanır
    Component component = componentService.getComponentById(id);
    DtoComponent dto = componentMapper.toDtoComponent(component);
    return RootEntityResponse.ok(dto);
  }

  @PostMapping
  public RootEntityResponse<DtoComponent> createComponent(
      @Valid @RequestBody DtoComponentIU dto) {
    // @Valid annotation otomatik validation yapar
    // Hata varsa GlobalExceptionHandler yakalar
    Component component = componentService.saveComponent(dto);
    return RootEntityResponse.ok(componentMapper.toDtoComponent(component));
  }
}
```

## En İyi Pratikler

### 1. Doğru Exception Seçimi
```java
// ✅ İyi
throw new ResourceNotFoundException("User", id);

// ❌ Kötü
throw new RuntimeException("User not found");
```

### 2. Anlamlı Mesajlar
```java
// ✅ İyi
throw new ValidationException("BANNER tipindeki component'e widget eklenemez");

// ❌ Kötü
throw new ValidationException("Invalid operation");
```

### 3. Service Layer'da Exception Fırlatma
```java
// ✅ İyi - Service layer
@Service
public class UserService {
  public User getUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }
}

// ❌ Kötü - Controller'da try-catch
@RestController
public class UserController {
  @GetMapping("/{id}")
  public ResponseEntity<?> getUser(@PathVariable Long id) {
    try {
      User user = userService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
```

### 4. Validation Annotations Kullanımı
```java
// ✅ İyi - Otomatik validation
public class CreateUserRequest {
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;
  
  @NotBlank(message = "Password is required")
  @Size(min = 8, message = "Password must be at least 8 characters")
  private String password;
}

@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
  // Validation hataları otomatik yakalanır
  return userService.createUser(request);
}
```

## Logging

Tüm exception'lar otomatik olarak loglanır:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(...) {
    log.error("Resource not found: {}", ex.getMessage());
    // ...
  }
}
```

Log çıktısı:
```
2024-12-14 10:30:45.123 ERROR [...] GlobalExceptionHandler : Resource not found: Component with id 123 not found
```

## Test Örnekleri

### REST API Testleri
```bash
# 404 - Resource Not Found
curl http://localhost:8080/api/v1/components/999

# Response:
{
  "result": false,
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Component with id 999 not found"
}

# 400 - Validation Error
curl -X POST http://localhost:8080/api/v1/components \
  -H "Content-Type: application/json" \
  -d '{"name": "", "type": null}'

# Response:
{
  "result": false,
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "validationErrors": {
    "name": "must not be blank",
    "type": "must not be null"
  }
}
```

## Özet

Global Exception Handler ile:
- ✅ Merkezi hata yönetimi
- ✅ Tutarlı API responses
- ✅ Otomatik logging
- ✅ Daha temiz kod (controller'larda try-catch'e gerek yok)
- ✅ Client-friendly error messages
- ✅ Kolay debugging

Artık tüm service'lerinizde custom exception'ları kullanabilir, GlobalExceptionHandler bunları otomatik olarak yakalar ve uygun formatta döndürür!
