---
description: "Elly pattern'ine uygun yeni bir özellik/entity iskeleti oluşturur"
argument-hint: "<EntityAdı> [açıklama]"
allowed-tools: Read, Write, Edit, Glob, Grep
---

Elly CMS'in standart katmanlı mimarisine uygun şekilde `$ARGUMENTS` entity'si için tam iskelet oluştur.

Önce `src/main/java/com/cms/` altındaki mevcut bir entity'yi (örn. controller/impl, service/impl, mapper) referans olarak oku ve aynı package yapısını, aynı import stilini ve aynı annotation'ları kullan.

Oluşturulacak dosyalar:

1. **Entity** — `entity/XxxEntity.java`
   - `@Entity @Table(name = "xxx")`
   - Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`
   - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`

2. **Repository** — `repository/XxxRepository.java`
   - `extends JpaRepository<XxxEntity, Long>`

3. **IService** — `service/IXxxService.java`
   - CRUD method imzaları

4. **ServiceImpl** — `service/impl/XxxServiceImpl.java`
   - `@Service @RequiredArgsConstructor @Transactional`
   - `@Cacheable` okuma, `@CacheEvict(allEntries = true)` yazma

5. **DTO'lar** — `dto/request/XxxCreateRequest.java` ve `dto/response/XxxResponse.java`
   - Lombok veya record

6. **Mapper** — `mapper/XxxMapper.java`
   - `@Mapper(componentModel = "spring")`

7. **IController** — `controller/IXxxController.java`
   - `@RequestMapping("/api/xxx")`

8. **ControllerImpl** — `controller/impl/XxxController.java`
   - `@RestController @RequiredArgsConstructor`
   - Tüm response'lar `RootEntityResponse<T>` ile wrap edilmeli
