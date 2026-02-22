# Global Exception Handler - Kurulum Özeti

## ✅ Tamamlanan İşlemler

### 1. Exception Yapısı Oluşturuldu

#### Exception Sınıfları (`com.cms.exception` paketi):
- ✅ `BaseException.java` - Temel exception sınıfı
- ✅ `ResourceNotFoundException.java` - 404 hataları için
- ✅ `BadRequestException.java` - 400 hataları için  
- ✅ `ValidationException.java` - Validasyon hataları için
- ✅ `ConflictException.java` - 409 çakışma hataları için
- ✅ `ErrorResponse.java` - Standart error response modeli
- ✅ `GlobalExceptionHandler.java` - Merkezi exception handler (@RestControllerAdvice)

### 2. Tüm Service Sınıfları Güncellendi

Aşağıdaki service'lerde `RuntimeException` yerine yeni custom exception'lar kullanılıyor:

#### ✅ ComponentService
```java
// Öncesi:
throw new RuntimeException("Component not found");

// Sonrası:
throw new ResourceNotFoundException("Component", id);
throw new ValidationException("BANNER tipindeki component'e widget eklenemez");
```

#### ✅ PageService
```java
throw new ResourceNotFoundException("Page", id);
throw new ResourceNotFoundException("Page", "slug", slug);
```

#### ✅ PostService
```java
throw new ResourceNotFoundException("Post", id);
```

#### ✅ BannerService
```java
throw new ResourceNotFoundException("Banner", id);
```

#### ✅ WidgetService
```java
throw new ResourceNotFoundException("Widget", id);
throw new ValidationException("BANNER tipindeki widget'a post eklenemez");
```

#### ✅ RatingService
```java
throw new ResourceNotFoundException("Rating", id);
```

#### ✅ CommentService
```java
throw new ResourceNotFoundException("Comment", id);
```

#### ✅ AssetsService
```java
throw new ResourceNotFoundException("Assets", id);
throw new ResourceNotFoundException("Assets", "name", name);
```

#### ✅ FileService
```java
throw new BadRequestException("File is empty or null");
throw new BadRequestException("File is not an image");
throw new BadRequestException("Failed to save image file", e);
```

### 3. Global Exception Handler Özellikleri

#### Otomatik Yakalanan Exception'lar:
- ✅ Custom BaseException ve alt sınıfları
- ✅ `MethodArgumentNotValidException` - @Valid validation hataları
- ✅ `ConstraintViolationException` - Constraint ihlalleri
- ✅ `DataIntegrityViolationException` - Database constraint hataları
- ✅ `HttpMessageNotReadableException` - Geçersiz JSON
- ✅ `MethodArgumentTypeMismatchException` - Yanlış parametre tipi
- ✅ `MissingServletRequestParameterException` - Eksik parametre
- ✅ `HttpRequestMethodNotSupportedException` - Desteklenmeyen HTTP method
- ✅ `NoHandlerFoundException` / `NoResourceFoundException` - 404 hataları
- ✅ `RuntimeException` - Genel runtime hataları
- ✅ `Exception` - Tüm diğer beklenmeyen hatalar

#### Özellikler:
- ✅ Otomatik logging (SLF4J/Lombok @Slf4j)
- ✅ Tutarlı JSON response formatı
- ✅ HTTP status code yönetimi
- ✅ Validation errors mapping
- ✅ Timestamp ve request path bilgisi

### 4. Error Response Formatı

#### Basit Hata:
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

#### Validation Hataları:
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
    "type": "must not be null"
  }
}
```

### 5. Dokümantasyon
- ✅ `GLOBAL_EXCEPTION_HANDLER.md` - Detaylı kullanım kılavuzu
- ✅ `EXCEPTION_IMPLEMENTATION_SUMMARY.md` - Bu özet dosyası

## 📊 Derleme Sonucu

```
[INFO] BUILD SUCCESS
[INFO] Total time:  6.193 s
```

✅ Proje başarıyla derlendi!
⚠️ Sadece MapStruct unmapped property uyarıları var (kritik değil)

## 🎯 Kullanım Örnekleri

### Service'de Exception Fırlatma

```java
@Service
public class ComponentService {
    
    // 404 - Resource Not Found
    public Component getComponentById(Long id) {
        return componentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Component", id));
    }
    
    // 400 - Validation Error
    public Component saveComponent(Component component, List<Long> widgetIds) {
        if (component.getType() == ComponentTypeEnum.BANNER 
            && widgetIds != null && !widgetIds.isEmpty()) {
            throw new ValidationException("BANNER tipindeki component'e widget eklenemez");
        }
        return componentRepository.save(component);
    }
    
    // 400 - Bad Request
    public void uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or null");
        }
        // ...
    }
    
    // 409 - Conflict
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ConflictException("A user with this email already exists");
        }
        return userRepository.save(user);
    }
}
```

### Controller'da Kullanım

Controller'da artık try-catch bloklarına gerek yok!

```java
@RestController
@RequestMapping("/api/v1/components")
public class ComponentController {

    @Autowired
    private IComponentService componentService;

    @GetMapping("/{id}")
    public RootEntityResponse<DtoComponent> getComponentById(@PathVariable Long id) {
        // Exception otomatik yakalanır!
        Component component = componentService.getComponentById(id);
        DtoComponent dto = componentMapper.toDtoComponent(component);
        return RootEntityResponse.ok(dto);
    }

    @PostMapping
    public RootEntityResponse<DtoComponent> createComponent(
            @Valid @RequestBody DtoComponentIU dto) {
        // @Valid validation hataları otomatik yakalanır!
        Component component = componentService.saveComponent(dto);
        return RootEntityResponse.ok(componentMapper.toDtoComponent(component));
    }
}
```

### Validation Annotations

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

## 🧪 Test Örnekleri

### 1. Resource Not Found (404)
```bash
curl http://localhost:8080/api/v1/components/999

# Response:
{
  "result": false,
  "status": 404,
  "error": "Not Found",
  "errorCode": "RESOURCE_NOT_FOUND",
  "message": "Component with id 999 not found",
  "path": "/api/v1/components/999"
}
```

### 2. Validation Error (400)
```bash
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

### 3. Custom Validation (400)
```bash
curl -X POST http://localhost:8080/api/v1/components \
  -H "Content-Type: application/json" \
  -d '{"name": "Test", "type": "BANNER", "widgetIds": [1, 2]}'

# Response:
{
  "result": false,
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_ERROR",
  "message": "BANNER tipindeki component'e widget eklenemez"
}
```

### 4. Database Constraint Violation (409)
```bash
# Duplicate key insertion
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email": "existing@example.com"}'

# Response:
{
  "result": false,
  "status": 409,
  "error": "Conflict",
  "errorCode": "DATA_INTEGRITY_VIOLATION",
  "message": "A record with this value already exists"
}
```

## 📁 Yeni Dosyalar

```
src/main/java/com/cms/exception/
├── BaseException.java
├── ResourceNotFoundException.java
├── BadRequestException.java
├── ValidationException.java
├── ConflictException.java
├── ErrorResponse.java
└── GlobalExceptionHandler.java

Dokümantasyon:
├── GLOBAL_EXCEPTION_HANDLER.md
└── EXCEPTION_IMPLEMENTATION_SUMMARY.md
```

## 🔄 Güncellenen Dosyalar

```
src/main/java/com/cms/service/impl/
├── ComponentService.java
├── PageService.java
├── PostService.java
├── BannerService.java
├── WidgetService.java
├── RatingService.java
├── CommentService.java
├── AssetsService.java
└── FileService.java
```

## ✨ Avantajlar

1. **Merkezi Yönetim**: Tüm hatalar tek bir yerden yönetiliyor
2. **Tutarlı API**: Tüm endpoint'ler aynı formatta hata dönüyor
3. **Temiz Kod**: Controller'larda try-catch bloklarına gerek yok
4. **Otomatik Logging**: Tüm hatalar otomatik loglanıyor
5. **Detaylı Bilgi**: Client'a anlamlı ve detaylı hata mesajları
6. **Type Safety**: Custom exception sınıfları kullanımı
7. **HTTP Standards**: Doğru HTTP status code'ları
8. **Validation Support**: Bean validation otomatik çalışıyor

## 📚 Detaylı Dokümantasyon

Daha fazla bilgi ve örnek için `GLOBAL_EXCEPTION_HANDLER.md` dosyasına bakın.

## 🎉 Sonuç

Global exception handler sisteminiz tamamen kurulmuş ve projenize entegre edilmiştir. Artık tüm servislerinizde custom exception'ları güvenle kullanabilirsiniz!

---
**Kurulum Tarihi**: 14 Aralık 2024  
**Spring Boot Version**: 3.5.7  
**Java Version**: 21
