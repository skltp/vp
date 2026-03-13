# Copilot Coding Agent Instructions for VP (VirtualiseringsPlattformen)

## Project Summary & Domain Context
VP is a SOAP/HTTP message-routing intermediary in the Swedish national health IT platform (SKLTP/NTjP). Consumers call VP instead of producers directly; VP authenticates the caller (mTLS certificate or HTTP headers), parses the SOAP envelope to extract the **logical address** (receiverId), **service contract namespace**, and **RIV-TA version**, resolves the physical **producer address** via the **TAK cache** (Tjänsteadresseringskatalogen), checks **authorization** (behörighet) with HSA organisational-tree traversal, and forwards the request. Errors are returned as standardised SOAP faults (VP001–VP015, defined in `vp-messages.properties`).

**Key domain terms:** *vägval* = routing entry, *behörighet* = authorization entry, *TAK* = Service Addressing Catalogue, *HSA* = national organisational directory, *RIV-TA* = interoperability profile, *logical address / receiverId* = the HSA-ID of the target organisation, *senderId* = consumer HSA-ID extracted from certificate `SERIALNUMBER`.

## Tech Stack
Java 17 · Spring Boot 3.4.3 · Apache Camel 4.8.5 · Netty HTTP (transport) · Undertow (actuator/management) · Maven 3.9+ (multi-module) · Lombok · Log4j2 (ECS + legacy layouts) · JUnit 5 + `camel-test-spring-junit5`

## Module Layout
| Module | Purpose |
|---|---|
| **`vp-services-camel`** | Main application — all routes, processors, configs, tests. |
| **`vp-wsdl-utils`** | Small utility for WSDL/XML parsing. Must be built before `vp-services-camel`. |
| **`report`** | JaCoCo aggregate coverage (no source code). |

All business logic lives under `vp-services-camel/src/main/java/se/skl/tp/vp/`.

### Source packages at a glance
| Package | Responsibility |
|---|---|
| `VPRouter.java` | Main Camel route definitions (inbound HTTP/HTTPS → vagval → producer). |
| `BeansConfiguration.java` | Central `@Configuration` — component-scans external libs, wires Netty pools, selects MessageLogger. |
| `vagval/` | `VagvalProcessor` (route lookup) and `BehorighetProcessor` (authorization check). |
| `errorhandling/` | SOAP fault generation, exception mapping. `VpCodeMessages` loads `vp-messages.properties`. |
| `exceptions/` | `VpSemanticErrorCodeEnum` (VP001–VP015), `VpSemanticException`, `VpTechnicalException`. |
| `certificate/` | Client certificate extraction (`SERIALNUMBER` field). |
| `httpheader/` | Sender-ID, original-consumer-ID, outbound header processing, IP whitelist. |
| `constants/` | `HttpHeaders`, `VPExchangeProperties` (Camel exchange property keys), `PropertyConstants`. |
| `config/` | `@ConfigurationProperties` classes for HTTP headers, TLS, HSA lookup, default routing. |
| `logging/` | `MessageLogger` interface with `EcsMessageLogger` and deprecated `LegacyMessageInfoLogger`. |
| `timeout/` | Per-service-contract timeout configuration (loaded from `timeoutconfig.json`). |
| `wsdl/` | WSDL serving (`?wsdl` support). |
| `sslcontext/` | Dynamic SSL context selection for outbound HTTPS calls. |

### External libraries (component-scanned)
`se.skltp.takcache`, `se.skl.tp.hsa.cache`, `se.skl.tp.behorighet`, `se.skl.tp.vagval` — scanned via `@ComponentScan` in `BeansConfiguration`.

## Build Commands (run from repo root `vp/`)
```
mvn clean install -DskipTests        # Compile all modules (~2 min)
mvn verify                            # Unit + integration tests (~5 min)
mvn test -pl vp-services-camel -Dtest=ClassName          # Single unit test
mvn verify -pl vp-services-camel -Dit.test=ClassName     # Single IT test
```
No external services needed — tests use embedded mocks (`MockProducer`, `TakMockWebService`).

### Known safe-to-ignore warnings
- `MockBean has been deprecated` — existing test code uses the old annotation.
- `HttpObjectDecoder.java uses or overrides a deprecated API` — intentional Netty classpath override; **do not delete** `vp-services-camel/src/main/java/io/netty/handler/codec/http/HttpObjectDecoder.java`.

## Coding Conventions

### Camel Route Architecture
Inbound routes (`vp-http-route`, `vp-https-route`) → `direct:vp` (vagval-route) → `direct:to-producer` (producer-route). Global `onException(Exception.class)` handles SOAP fault generation. Producer-route has specific exception handlers for `SocketException` (retry), `ReadTimeoutException`, `SSLHandshakeException`, etc.

### Error Handling
All errors become SOAP faults with codes VP001–VP015. Throw `VpSemanticException` (for client/business errors) or `VpTechnicalException` (for infrastructure errors) using `ExceptionUtil`. Never expose raw stack traces in responses.

### Test Organisation
- **Unit tests** (`*Test.java`): mirror `src/main` packages. Run by Surefire. Most use `@CamelSpringBootTest` + `@SpringBootTest` with a test-specific `@Configuration` class. Some use `@ExtendWith(MockitoExtension.class)`.
- **Integration tests** (`*IT.java`): in `se.skl.tp.vp.integrationtests`. Run by Failsafe. Require:
  - `@CamelSpringBootTest`, `@SpringBootTest`, `@DirtiesContext(classMode = ClassMode.AFTER_CLASS)`
  - `@StartTakService` (activates `StartTakService` Spring profile for the TAK mock)
  - Extend `LeakDetectionBaseTest` (Netty buffer leak detection)
  - Call `TestLogAppender.clearEvents()` in `@BeforeEach`
- Use `JunitUtil.assertStringContains()` for substring assertions.
- `MockProducer` and `TestConsumer` are Spring `@Component` beans — `@Autowired` in tests.
- `@SuppressWarnings("SpringBootApplicationProperties")` on test classes using `@TestPropertySource` with custom VP properties.

### Properties
Custom properties (prefixed `vp.`, `producer.`, `tp.tls.`, etc.) are defined in `application.properties` and `application-security.properties`. Test overrides are in `src/test/resources/application.properties`. `vp.logging.style` toggles `ECS` (test default) vs. `LEGACY` (prod default) message logger.

### Dependencies
All versions are pinned in the **parent POM** `<dependencyManagement>`. Child modules must **not** declare versions. Lombok and `spring-boot-configuration-processor` are `<optional>true</optional>`.

### Code Style
- Constructor injection via `@RequiredArgsConstructor` (Lombok). Prefer `final` fields.
- Processors implement `org.apache.camel.Processor`.
- Services annotated `@Service`; configs annotated `@Configuration` or `@ConfigurationProperties`.
- Return empty collections over `null`. Use `StandardCharsets.UTF_8` explicitly.
- Structured logging (ECS JSON). Never log secrets, PII, or raw user input.

## Validation Checklist
After every change:
1. `mvn clean install -DskipTests` — must compile without errors.
2. `mvn verify` — all unit and integration tests must pass.
3. For single-module changes: `mvn verify -pl vp-services-camel` (requires `vp-wsdl-utils` already installed in local repo).

