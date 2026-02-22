Act as a Senior Backend Developer specializing in Java, Spring Boot 3, and PostgreSQL.

I need you to generate a backend structure for a flexible, industry-agnostic Headless CMS.
The core requirement is to handle highly variable content structures (e.g., a "Dentist Treatment" has an icon and description, while a "Developer Experience" has a company name, period, technologies array, and achievements list) within a single database table using PostgreSQL's JSONB capabilities.

### Tech Stack
- Java 21
- Spring Boot 3.x
- PostgreSQL (using the `jsonb` data type)
- Spring Data JPA (Hibernate 6)
- Lombok
- Maven

### Database Schema Requirement
Create a single table named `cms_contents`.
- `id`: UUID (Primary Key)
- `section_key`: String (e.g., "homepage_hero", "resume_experience", "services_grid") - used for fetching grouped data.
- `content_type`: String (e.g., "treatment_card", "experience_item", "testimonial") - used by the frontend to decide which component to render.
- `title`: String (Common field for searching/admin listing).
- `is_active`: Boolean.
- `sort_order`: Integer.
- `metadata`: JSONB (This is the critical part. It should map to `Map<String, Object>` in Java and store all variable fields like icons, descriptions, arrays, ratings, etc.)

### Implementation Details
1. **Entity:** Create the `CmsContent` entity. 
   - Use `@JdbcTypeCode(SqlTypes.JSON)` from Hibernate 6 to handle the `metadata` field as a `Map<String, Object>`. Do not use the deprecated `vladmihalcea` library if Hibernate 6 native support is sufficient.
2. **Repository:** Create a `CmsContentRepository`.
   - Add a method to find content by `sectionKey`, ordered by `sortOrder`.
   - Add a method to find active content only.
3. **Service:** Create a `CmsContentService` with standard CRUD operations.
4. **Controller:** Create `CmsContentController` with REST endpoints:
   - GET `/api/v1/contents/{sectionKey}` (Returns list of items for a specific section)
   - POST `/api/v1/contents` (Create new item)
   - PUT `/api/v1/contents/{id}`
   - DELETE `/api/v1/contents/{id}`

### Context & Data Examples
To understand the flexibility required, here are two different JSON structures that must be saved into the `metadata` column of this same table:

**Example 1: Dentist Service**
```json
{
  "icon": "Syringe",
  "desc": "Permanent solution for missing teeth"
}