Act as a Principal Java Engineer and Spring Boot Architect. I need you to implement the backend infrastructure for a "Schema-Driven UI" (Dynamic Form & Survey) system.

**Tech Stack:**
* Java 21
* Spring Boot 3.x
* PostgreSQL
* Hibernate 6 (JPA)
* Library: `io.hypersistence:hypersistence-utils-hibernate-63` (For JSONB support)
* Lombok

**Context:**
We are building a system where forms are defined as JSON schemas in the database, and submissions are stored as JSON payloads. The backend must enforce validation rules dynamically based on the stored schema.

**1. Database Entities (JPA)**
Create two entities:
* `FormDefinition`:
    * `id` (UUID, PK)
    * `title` (String)
    * `version` (Integer)
    * `schema` (JSONB - maps to a POJO `FormSchema`)
    * `active` (Boolean)
* `FormSubmission`:
    * `id` (UUID, PK)
    * `formDefinition` (ManyToOne, Lazy)
    * `payload` (JSONB - maps to `Map<String, Object>`)
    * `submittedAt` (LocalDateTime)

**2. The Schema Contract (POJOs)**
Create the necessary POJOs to map the `schema` JSONB column. The structure should support:
* `FormSchema`: contains `config` (layout type etc.) and `fields` (List of FieldDefinition).
* `FieldDefinition`: contains `id`, `type` (text, select, number), `label`, `required` (boolean), `validation` (ValidationRule object), and `condition` (ConditionRule object).
* `ValidationRule`: min, max, regex pattern.
* `ConditionRule`: used for conditional logic (e.g., show field B if field A equals "X"). Contains `field`, `operator` (EQUALS, NOT_EQUALS, GT, LT), and `value`.

**3. Business Logic Requirements (Crucial)**

* **A. Conditional Logic Engine (`ConditionEvaluator`):**
    Create a component that evaluates if a field should be visible/active based on the `ConditionRule` and the user's current answers (`payload`).
    * Logic: If `rule.field`'s value in the payload matches the `rule.operator` and `rule.value`, return true.

* **B. Dynamic Validation Strategy (`FieldValidator`):**
    Implement the Strategy Pattern.
    * Interface `FieldValidator`: `boolean supports(String type)` and `void validate(Object value, FieldDefinition def)`.
    * Implement concrete validators: `TextFieldValidator`, `NumberFieldValidator`, `SelectFieldValidator`.
    * *Important:* The validators should throw a custom `FormValidationException` if validation fails.

* **C. Submission Service (`DynamicFormService`):**
    Implement a `submitForm(UUID formId, Map<String, Object> payload)` method.
    * **Step 1:** Fetch the `FormDefinition`.
    * **Step 2:** Iterate through `form.schema.fields`.
    * **Step 3:** Use `ConditionEvaluator` to check if the field is visible.
        * *If NOT visible:* Remove the key from the `payload` (Sanitization) to prevent storing irrelevant data, and SKIP validation.
        * *If visible:* Find the correct `FieldValidator` and execute validation.
    * **Step 4:** If all validations pass, save the sanitized payload to `FormSubmission`.

**Output Instructions:**
* Provide the full code for Entities, POJOs, Validator Interface/Implementations, Evaluator, and the Service.
* Use standard Spring Boot annotations (`@Service`, `@Component`, `@Repository`).
* Ensure clean code principles and proper error handling.