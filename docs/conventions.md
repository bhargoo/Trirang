# TriRang Development Conventions

This document establishes the ground rules for all development (Person A and Person B) to avoid merge conflicts, architecture drift, and duplicate work.

## 1. Protected Files (Core Infrastructure)
These files are owned entirely by **Person A**. Person B should NOT modify these files. If Person B needs a change, they must request it from Person A.
- `src/main/java/.../config/SecurityConfig.java`
- `src/main/java/.../security/JwtFilter.java`
- `src/main/java/.../exception/GlobalExceptionHandler.java`
- `pom.xml`
- `src/main/resources/application.yml` and `application-dev.yml`
- Shared core enums (e.g., `Role.java`)

## 2. Package Structure & Naming
We enforce a strict package structure. Do not create random packages.
- **Controllers:** `com.trirang.controller`
- **Services:** `com.trirang.service` (Interfaces and Impls if necessary, though direct classes are preferred to avoid excessive abstraction)
- **Repositories:** `com.trirang.repository`
- **Entities:** `com.trirang.model.entity`
- **Enums:** `com.trirang.model.enums` (Shared enums in `com.trirang.model.enums.shared`)
- **Mappers:** `com.trirang.mapper` (Using MapStruct)

## 3. DTO Naming Conventions (Freeze Early!)
To prevent AI hallucination and mapping issues later, stick to these suffixes strictly:
- **Requests:** `[Action]Request` (e.g., `RegisterRequest`, `CreateDonationRequest`) -> Stored in `com.trirang.model.dto.request`
- **Responses:** `[Entity]Response` (e.g., `UserResponse`, `DonationResponse`) -> Stored in `com.trirang.model.dto.response`
- **Do not** use generic `*Dto` names unless it's a nested object within a request/response.

## 4. API Response Format & Validation
- **Standard Responses:** Controllers should return `ResponseEntity<T>`. 
- **Validation:** Use Spring Validation (`@Valid`, `@NotBlank`, `@NotNull`) directly in the DTOs.
- **Error Handling:** Let the `GlobalExceptionHandler` intercept exceptions and return a standard `ProblemDetail` or `ErrorResponse`. **Do not** write try-catch blocks in controllers for standard business logic errors. Throw custom runtime exceptions (e.g., `ResourceNotFoundException`) instead.

## 5. Pagination Rules
- Always use Spring Data's `Pageable`.
- Zero-indexed pages (Spring default).
- Return custom wrapper `PageResponse<T>` if you need standard meta-data, or standard `Page<T>` inside the `ResponseEntity`.

## 6. Flyway Migrations
- **Owner:** Person A owns migration numbering to prevent `V5` + `V5` conflicts.
- **Naming:** `V<version>__<description>.sql` (e.g., `V1__init_schema.sql`).
- If Person B needs a new table, they write the SQL and coordinate with Person A on the version number before pushing.

## 7. Git Workflow (Mandatory before Push)
1. `mvn clean compile`
2. `mvn test` (if applicable)
3. **Never merge broken code.**
4. When merging to `develop`, always pull `develop` into your feature branch first, fix conflicts locally, ensure it compiles, and *then* merge/push.
