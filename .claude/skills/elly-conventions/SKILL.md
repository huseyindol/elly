---
name: elly-conventions
description: Elly CMS'e özel kod yazma kuralları. Yeni Java sınıfı, endpoint, entity veya service oluştururken ya da mevcut Elly kodunu düzenlerken otomatik olarak aktif et.
version: 1.0.0
---

# Elly CMS Kod Konvansiyonları

Elly CMS için kod yazarken şu kurallara uy:

## Package Yapısı
```
com.cms.
├── config/          # Spring konfigürasyonları
├── controller/      # IXxxController interface
│   └── impl/        # XxxController implementasyonları
├── service/         # IXxxService interface
│   └── impl/        # XxxServiceImpl implementasyonları
├── repository/      # Spring Data JPA repository'leri
├── entity/          # JPA @Entity sınıfları
├── mapper/          # MapStruct mapper interface'leri
├── dto/
│   ├── request/     # İstek DTO'ları (CreateRequest, UpdateRequest)
│   └── response/    # Yanıt DTO'ları (XxxResponse)
├── exception/       # Custom exception sınıfları
├── enums/           # Enum tanımları
└── util/            # Yardımcı sınıflar
```

## Zorunlu Pattern
```java
// Entity
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "xxx")
public class XxxEntity { ... }

// Service impl
@Service @RequiredArgsConstructor @Transactional
public class XxxServiceImpl implements IXxxService {
    private final XxxRepository repository;
    private final XxxMapper mapper;
}

// Controller impl
@RestController @RequiredArgsConstructor
public class XxxController implements IXxxController {
    private final IXxxService service;

    public RootEntityResponse<XxxResponse> getById(Long id) {
        return RootEntityResponse.success(service.getById(id));
    }
}

// Mapper
@Mapper(componentModel = "spring")
public interface XxxMapper {
    XxxResponse toResponse(XxxEntity entity);
    XxxEntity toEntity(XxxCreateRequest request);
}
```

## Yasak Pratikler
- Entity'yi doğrudan controller'dan döndürme
- `@Autowired` field injection kullanma
- Tenant context olmadan cross-tenant query yazma
- Controller içinde `@Transactional` kullanma
- Cache key'lerinde tenantId'yi atlamak (multi-tenant cache'ler için)
