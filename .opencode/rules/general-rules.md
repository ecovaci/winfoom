# Project Rules

These rules apply to every agent and every interaction in this project.
No agent may override them. When a rule conflicts with a user request,
flag the conflict before proceeding.

---

## 1. Code Style

- Use **constructor injection** exclusively. Never use `@Autowired` on fields.
- Use **Java records** for DTOs and value objects. No Lombok `@Data` on DTOs.
- Lombok is permitted only on entities (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
- All classes must be **package-private** unless they need to be public. Default to the least permissive access modifier.
- No wildcard imports (`import com.example.*`).
- Method length: if a method exceeds ~30 lines, flag it for extraction without being asked.

---

## 2. Spring Conventions

- `@Transactional` belongs on the **service layer**, never on controllers or repositories.
- All state-modifying service methods must be annotated with `@Transactional`.
- All read-only service methods must be annotated with `@Transactional(readOnly = true)`.
- Services depend on **interfaces**, never on concrete implementations directly.
- Controllers are thin: no business logic, no direct repository access, no entity exposure.
- Never expose JPA entities in API responses. Always map to a DTO before returning.

---

## 3. Data & Persistence

- Default fetch strategy for all `@OneToMany` and `@ManyToMany`: `FetchType.LAZY`.
- Never use `FetchType.EAGER`. If eager loading is needed, use JPQL with `JOIN FETCH` or `@EntityGraph`.
- Entities must not contain business logic beyond simple field validation.
- Database migrations are handled via Liquibase. Never modify the schema manually or via `ddl-auto=update` in non-local environments.
- All queries beyond simple CRUD must be in the repository as named methods or `@Query`. No query construction in the service layer.

---

## 4. Error Handling

- All exceptions bubble up to a single `@RestControllerAdvice` global handler. No `try/catch` in controllers.
- Business exceptions extend a base `AppException` (or equivalent project exception hierarchy).
- Never expose stack traces or internal messages in API error responses.
- HTTP status codes must be semantically correct: 400 for client error, 404 for not found, 409 for conflict, 500 for unexpected server error.

---

## 5. Validation

- All request DTOs must use Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, etc.).
- Controllers must annotate request body parameters with `@Valid`.
- Custom validators go in a dedicated `validation` package.

---

## 6. Testing

- Unit tests: plain JUnit 5 + Mockito. No Spring context for service-layer unit tests.
- Integration tests: `@SpringBootTest` + Testcontainers for database tests.
- Controller tests: `@WebMvcTest` + MockMvc.
- Test method naming convention: `methodName_stateUnderTest_expectedBehavior`.
- Every new service method must have at least one happy-path and one failure-path test.

---

## 7. Security

- Never log sensitive fields: passwords, tokens, PII.
- Never hardcode credentials or secrets. Use environment variables or a secrets manager.
- All endpoints must have explicit security configuration. No security by convention or default.

---

## 8. AI Interaction Rules

- Never generate code that silently skips validation, error handling, or transactions.
- When introducing a library or pattern not already present in the project, flag it explicitly before using it.
- When requirements are ambiguous, ask ONE clarifying question before generating code.
- Always show full class files unless the user explicitly asks for a snippet.
- State what you are about to generate in one sentence before generating it.
