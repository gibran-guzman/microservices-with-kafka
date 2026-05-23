# microservices-with-kafka

Mini arquitectura empresarial basada en microservicios con Spring Boot, Eureka, Gateway, Kafka y Docker Compose.

---

## Roadmap de Implementación

### Fase 0 — Inicialización del proyecto

**Duración estimada:** 1 hora  
**Dependencias:** Ninguna  
**Prioridad:** MVP

| Tarea | Descripción |
|---|---|
| 0.1 | Crear estructura de directorios `/microservices-with-kafka` con subdirectorios por módulo: `eureka-server/`, `gateway-server/`, `customer-service/`, `product-service/`, `order-service/`, `notification-service/` |
| 0.2 | Generar cada proyecto Spring Boot con Spring Initializr o manual con Maven `pom.xml` (Java 17, Spring Boot 3.x, Spring Cloud 2023.x) |
| 0.3 | Configurar `pom.xml` raíz opcional como multi-módulo Maven |
| 0.4 | Configurar `.gitignore` estándar para Java + Maven + Docker |
| 0.5 | Definar versiónes de dependencias en `<properties>` o BOM de Spring Cloud |

**Criterios de aceptación:**
- Cada módulo compila con `mvn clean compile -pl <module>` sin errores
- Estructura de carpetas consistente (controller/service/repository/entity/dto/config/exception)

**Riesgos:**
- Incompatibilidad entre versiones de Spring Boot y Spring Cloud — usar BOM oficial `spring-cloud-dependencies:2023.0.x`

---

### Fase 1 — Infraestructura base: Eureka + Gateway

**Duración estimada:** 2-3 horas  
**Dependencias:** Fase 0  
**Prioridad:** MVP

#### 1.1 Eureka Server

| Tarea | Descripción |
|---|---|
| 1.1.1 | Crear `eureka-server/` con dependencia `spring-cloud-starter-netflix-eureka-server` |
| 1.1.2 | Configurar `application.yml`: puerto `8761`, `registerWithEureka=false`, `fetchRegistry=false` |
| 1.1.3 | Anotar clase principal con `@EnableEurekaServer` |
| 1.1.4 | Crear perfiles: `dev` (local), `docker` (contenedor) |

```yaml
# application.yml
server:
  port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
```

#### 1.2 Gateway Server

| Tarea | Descripción |
|---|---|
| 1.2.1 | Crear `gateway-server/` con dependencias: `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client` |
| 1.2.2 | Configurar `application.yml`: puerto `8080`, nombre `GATEWAY-SERVER`, registro en Eureka |
| 1.2.3 | Definir rutas estáticas iniciales con `lb://` (sin servicios aún, validar que fallen gracefulmente) |
| 1.2.4 | Anotar con `@EnableDiscoveryClient` |

```yaml
spring:
  application:
    name: GATEWAY-SERVER
  cloud:
    gateway:
      routes:
        - id: customer-service
          uri: lb://CUSTOMER-SERVICE
          predicates:
            - Path=/api/clientes/**
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/productos/**
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/pedidos/**
```

#### 1.3 Docker Compose inicial (soporte)

| Tarea | Descripción |
|---|---|
| 1.3.1 | Crear `docker-compose.yml` con servicios `eureka-server` y `gateway-server` |
| 1.3.2 | Verificar que Eureka dashboard es accesible en `http://localhost:8761` |
| 1.3.3 | Verificar que Gateway responde en `http://localhost:8080` |

**Criterios de aceptación:**
- `http://localhost:8761` muestra el dashboard de Eureka
- Gateway inicia y se registra en Eureka (visible en dashboard)
- `docker-compose up eureka-server gateway-server` levanta ambos sin errores

**Riesgos:**
- Gateway sin rutas válidas puede fallar al iniciar si los servicios destino no existen — usar filtros o config `spring.cloud.gateway.loadbalancer.use404=true` en desarrollo

---

### Fase 2 — Servicios de negocio: customer-service y product-service

**Duración estimada:** 4-5 horas  
**Dependencias:** Fase 1 (Eureka + Gateway deben estar operativos)  
**Prioridad:** MVP

#### 2.1 customer-service

| Tarea | Descripción |
|---|---|
| 2.1.1 | Crear módulo con dependencias: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-cloud-starter-netflix-eureka-client`, `h2` (dev) / `postgresql` (docker), `spring-boot-starter-validation`, `lombok` |
| 2.1.2 | Definir entidad `Cliente` (id, nombre, correo) |
| 2.1.3 | Crear `ClienteRepository` (Spring Data JPA) |
| 2.1.4 | Crear `ClienteDTO` (request/response) y `ClienteMapper` |
| 2.1.5 | Crear `ClienteService` con lógica CRUD |
| 2.1.6 | Crear `ClienteController` con endpoints REST |
| 2.1.7 | Implementar `GlobalExceptionHandler` con `@RestControllerAdvice` |
| 2.1.8 | Configurar `application-dev.yml` (H2 en memoria, puerto 8081) y `application-docker.yml` (PostgreSQL) |
| 2.1.9 | Agregar validaciones Bean Validation en DTOs |
| 2.1.10 | Configurar `spring.application.name=CUSTOMER-SERVICE` |

#### 2.2 product-service

| Tarea | Descripción |
|---|---|
| 2.2.1 | Crear módulo con mismas dependencias base |
| 2.2.2 | Definir entidad `Producto` (id, nombre, precio, stock) |
| 2.2.3 | Crear capas repository, service, controller, DTOs, mapper |
| 2.2.4 | CRUD completo con validaciones |
| 2.2.5 | `GlobalExceptionHandler` propio |
| 2.2.6 | Perfiles dev/docker (H2 → PostgreSQL) |
| 2.2.7 | `spring.application.name=PRODUCT-SERVICE` |

#### 2.3 Integración con Gateway

| Tarea | Descripción |
|---|---|
| 2.3.1 | Agregar ambos servicios al `docker-compose.yml` |
| 2.3.2 | Verificar enrutamiento: `GET /api/clientes` → customer-service, `GET /api/productos` → product-service |
| 2.3.3 | Verificar registro en Eureka dashboard |

**Criterios de aceptación:**
- `POST /api/clientes` con `{"nombre":"Juan","correo":"juan@mail.com"}` → 201 + JSON creado
- `GET /api/clientes` → lista paginada/simple de clientes
- `PUT /api/clientes/{id}` actualiza correctamente
- `DELETE /api/clientes/{id}` → 204
- Mismos endpoints para productos con sus campos
- Todos los endpoints funcionan a través de Gateway (`localhost:8080/api/clientes/...`)
- Validaciones: campos vacíos retornan 400 con mensaje descriptivo
- Recurso inexistente retorna 404

**Riesgos:**
- Gateway con `lb://` requiere que Eureka tenga el servicio registrado — si el servicio tarda en registrarse, las primeras peticiones pueden fallar. Solución: `retry` filter en Gateway o `eureka.client.initial-instance-info-replication-interval-seconds=10`
- Conflictos de mapeo si dos servicios usan el mismo path base — usar context-path por servicio

---

### Fase 3 — Servicio de pedidos con Kafka Producer

**Duración estimada:** 3-4 horas  
**Dependencias:** Fase 2 (customer-service, product-service), Kafka en docker-compose  
**Prioridad:** MVP

#### 3.1 order-service

| Tarea | Descripción |
|---|---|
| 3.1.1 | Crear módulo con dependencias adicionales: `spring-kafka` |
| 3.1.2 | Definir entidad `Pedido` (id, clienteId, productoId, cantidad) |
| 3.1.3 | Crear `PedidoRepository` |
| 3.1.4 | Crear `PedidoDTO` (request/response) y `PedidoMapper` |
| 3.1.5 | Crear `order.event.OrderEvent` — POJO con contrato: `orderId`, `clienteId`, `productoId`, `cantidad`, `timestamp` |
| 3.1.6 | Crear `OrderEventProducer` — publica en Kafka topic `order-topic` |
| 3.1.7 | Configurar `application.yml` para Kafka producer: `bootstrap-servers`, `key-serializer`, `value-serializer` |
| 3.1.8 | En `PedidoService.create()`: persistir + publicar evento |
| 3.1.9 | `PedidoController` con `POST /pedidos`, `GET /pedidos`, `GET /pedidos/{id}` |
| 3.1.10 | `spring.application.name=ORDER-SERVICE` |
| 3.1.11 | Topic configurable via `app.kafka.topic=order-topic` |

#### 3.2 Infraestructura Kafka

| Tarea | Descripción |
|---|---|
| 3.2.1 | Agregar `zookeeper` y `kafka` al `docker-compose.yml` |
| 3.2.2 | Configurar health check para Kafka |
| 3.2.3 | Configurar red Docker para que servicios accedan a Kafka por nombre de contenedor |

```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.6.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000

kafka:
  image: confluentinc/cp-kafka:7.6.0
  depends_on:
    - zookeeper
  ports:
    - "9092:9092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

**Criterios de aceptación:**
- `POST /api/pedidos` con `{"clienteId":1,"productoId":1,"cantidad":2}` → 201 + pedido creado
- `GET /api/pedidos` lista pedidos
- Al crear pedido, se publica mensaje JSON en Kafka topic `order-topic`
- Se puede verificar publicación usando Kafka CLI o consumer de prueba
- servicio registrado en Eureka como `ORDER-SERVICE`

**Riesgos:**
- Kafka no disponible al arrancar — implementar retry en producer con `spring.kafka.producer.retries` y `acks=all`
- Serialización JSON incorrecta — usar `JsonSerializer` de Spring Kafka con type mapping explícito
- Transaccionalidad: si falla Kafka después de persistir en BD, hay inconsistencia — considerar patrón Outbox (post-MVP)

---

### Fase 4 — Servicio de notificaciones con Kafka Consumer

**Duración estimada:** 2-3 horas  
**Dependencias:** Fase 3 (Kafka topic `order-topic` debe existir)  
**Prioridad:** MVP

#### 4.1 notification-service

| Tarea | Descripción |
|---|---|
| 4.1.1 | Crear módulo con dependencias: `spring-boot-starter-web` (mínimo, solo para health), `spring-kafka`, `spring-cloud-starter-netflix-eureka-client` |
| 4.1.2 | Crear clase `OrderEvent` (mismo contrato que en order-service — duplicado intencional por desacoplamiento) |
| 4.1.3 | Crear `OrderEventConsumer` con `@KafkaListener(topics = "order-topic")` |
| 4.1.4 | Deserializar JSON a `OrderEvent` y loggear: `"Pedido recibido correctamente - OrderId: {}"` |
| 4.1.5 | Configurar `application.yml`: Kafka consumer con `group-id=notification-group`, `value-deserializer`, `auto-offset-reset=earliest` |
| 4.1.6 | `spring.application.name=NOTIFICATION-SERVICE` |
| 4.1.7 | Topic configurable via `app.kafka.topic=order-topic` |

#### 4.2 Integración

| Tarea | Descripción |
|---|---|
| 4.2.1 | Agregar `order-service` y `notification-service` al `docker-compose.yml` |
| 4.2.2 | Verificar dependencias: notification-service necesita Kafka, no order-service directamente |
| 4.2.3 | Probar flujo completo: crear pedido → ver log en notification-service |

**Criterios de aceptación:**
- `notification-service` inicia y se registra en Eureka
- Al crear un pedido via Gateway, el consumer de notification-service registra: `Pedido recibido correctamente`
- El consumo es desacoplado: si notification-service está caído, order-service sigue funcionando
- Mensaje se procesa cuando notification-service se recupera (por `auto-offset-reset=earliest`)

**Riesgos:**
- Deserialización: si el contrato del evento cambia en producer pero no en consumer, hay errores — definir contrato compartido o schema registry (post-MVP)
- Procesamiento duplicado: Kafka entrega al menos una vez — considerar idempotencia en consumer (post-MVP)

---

### Fase 5 — Dockerización completa y pruebas end-to-end

**Duración estimada:** 3-4 horas  
**Dependencias:** Fases 1-4 completas  
**Prioridad:** MVP

#### 5.1 Dockerfiles

| Tarea | Descripción |
|---|---|
| 5.1.1 | Crear `Dockerfile` para cada servicio usando `maven:3.9-eclipse-temurin-17` como build multi-stage |
| 5.1.2 | Build stage: `mvn clean package -DskipTests` |
| 5.1.3 | Runtime stage: `eclipse-temurin:17-jre-alpine` |

```dockerfile
# Ejemplo genérico
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 5.2 docker-compose.yml final

| Tarea | Descripción |
|---|---|
| 5.2.1 | Integrar todos los servicios: zookeeper, kafka, eureka-server, gateway-server, customer-service, product-service, order-service, notification-service |
| 5.2.2 | Agregar PostgreSQL por servicio o base compartida por esquemas (PostgreSQL para customer, product, order) |
| 5.2.3 | Usar `SPRING_PROFILES_ACTIVE=docker` en todos los servicios |
| 5.2.4 | Configurar depends_on: order-service → kafka, servicios de negocio → sus BDs |
| 5.2.5 | Agregar healthcheck a cada servicio |
| 5.2.6 | Red compartida para comunicación entre contenedores |

#### 5.3 Pruebas end-to-end

| Tarea | Descripción |
|---|---|
| 5.3.1 | Script de prueba: crear cliente → crear producto → crear pedido → verificar log de notificación |
| 5.3.2 | Verificar Eureka dashboard con 6 servicios registrados |
| 5.3.3 | Probar errores: 404 en recursos inexistentes, 400 en datos inválidos |
| 5.3.4 | Probar tolerancia: detener notification-service → crear pedido → reiniciar notification-service → verificar consumo |

**Criterios de aceptación:**
- `docker-compose up --build` levanta todo sin errores
- `http://localhost:8761` muestra 6 servicios registrados
- `POST http://localhost:8080/api/clientes` → 201
- `POST http://localhost:8080/api/productos` → 201
- `POST http://localhost:8080/api/pedidos` → 201 + evento Kafka publicado
- Logs de notification-service muestran `Pedido recibido correctamente`
- Health endpoints (`/actuator/health`) retornan UP para todos los servicios

**Riesgos:**
- Orden de arranque: Eureka primero, luego Gateway, luego servicios de negocio. Kafka debe estar listo antes que order-service y notification-service
- Tiempo de inicio: servicios pueden tardar >30s en JDK 17 — configurar `depends_on` con `condition: service_healthy`
- Puertos expuestos: asegurar que no haya conflictos en la máquina host

---

### Fase 6 — Observabilidad y hardening (Post-MVP)

**Duración estimada:** 4-6 horas  
**Dependencias:** Fase 5  
**Prioridad:** Post-MVP / Mejora continua

| Tarea | Descripción | Prioridad |
|---|---|---|
| 6.1 | Agregar Spring Boot Actuator con endpoints de health, info, metrics | Alta |
| 6.2 | Implementar logs estructurados con Logback JSON | Alta |
| 6.3 | Agregar `spring-boot-starter-aop` para logging de auditoría en servicios críticos | Media |
| 6.4 | Implementar patrón Outbox en order-service para consistencia eventual | Alta |
| 6.5 | Agregar Schema Registry o contrato compartido (Avro/Protobuf) | Baja |
| 6.6 | Implementar idempotencia en notification-service (deduplication key) | Alta |
| 6.7 | Agregar Circuit Breaker (Resilience4j) en Gateway y llamadas entre servicios | Media |
| 6.8 | Agregar seguridad básica: API Key en Gateway, CORS configurado | Media |
| 6.9 | Agregar `spring-cloud-sleuth` o Micrometer Tracing para trazabilidad distribuida | Baja |
| 6.10 | Pruebas unitarias con JUnit 5 + Mockito (cobertura mínima 70%) | Alta |
| 6.11 | Pruebas de integración con Testcontainers | Media |
| 6.12 | Linting y formato con Checkstyle/Spotless | Media |

**Criterios de aceptación:**
- Actuator health expone detalles de conectividad (Kafka, BD)
- Logs en formato JSON estructurado con traceId y spanId
- Al reiniciar notification-service no se duplican notificaciones
- Circuit breaker aísla fallos en Gateway
- Pruebas pasan en CI

**Riesgos:**
- Agregar demasiada complejidad post-MVP puede retrasar entregas — priorizar por valor
- Trazabilidad distribuida requiere instrumentar todos los servicios consistentemente

---

## Diagrama de dependencias entre fases

```
Fase 0 (Inicialización)
   │
   ▼
Fase 1 (Eureka + Gateway + Docker base)
   │
   ▼
Fase 2 (customer-service + product-service)
   │
   ▼
Fase 3 (order-service + Kafka producer)
   │
   ▼
Fase 4 (notification-service)
   │
   ▼
Fase 5 (Dockerización completa + E2E)
   │
   ▼
Fase 6 (Observabilidad + Hardening) ⬅ Post-MVP
```

---

## Matriz de servicios y puertos

| Servicio | Puerto interno | Exposición | BD (dev) | BD (docker) |
|---|---|---|---|---|
| eureka-server | 8761 | 8761 | — | — |
| gateway-server | 8080 | 8080 | — | — |
| customer-service | 8081 | — | H2 | postgres:5432/customerdb |
| product-service | 8082 | — | H2 | postgres:5432/productdb |
| order-service | 8083 | — | H2 | postgres:5432/orderdb |
| notification-service | 8084 | — | — | — |
| zookeeper | 2181 | 2181 | — | — |
| kafka | 9092 | 9092 | — | — |

---

## Estructura de directorios final esperada

```
microservices-with-kafka/
├── docker-compose.yml
├── eureka-server/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../EurekaServerApplication.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── application-docker.yml
├── gateway-server/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../GatewayServerApplication.java
│       └── resources/
│           ├── application.yml
│           └── application-docker.yml
├── customer-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/.../customer/
│       ├── CustomerServiceApplication.java
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── mapper/
│       ├── config/
│       └── exception/
├── product-service/  (misma estructura que customer)
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/.../order/
│       ├── OrderServiceApplication.java
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── mapper/
│       ├── config/
│       ├── exception/
│       └── event/        ← Kafka producer + OrderEvent
├── notification-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/.../notification/
│       ├── NotificationServiceApplication.java
│       ├── event/        ← OrderEvent (consumer)
│       ├── config/
│       └── exception/
└── README.md
```

---

## Resumen de prioridades MVP vs Post-MVP

| Aspecto | MVP | Post-MVP |
|---|---|---|
| **Funcionalidad** | CRUD + pedidos + Kafka + notificación | ⚡ |
| **Persistencia** | H2 en dev, PostgreSQL en docker | Migraciones Flyway/ Liquibase |
| **Gateway** | Rutas estáticas con `lb://` | Rate limiting, filtros personalizados |
| **Kafka** | Producer + Consumer simple | Schema Registry, Outbox, particionamiento |
| **Observabilidad** | Logs básicos + Actuator health | Tracing, métricas, Grafana/Prometheus |
| **Resiliencia** | Ninguna | Circuit Breaker, retry, bulkhead |
| **Seguridad** | Ninguna | API Key, OAuth2, HTTPS |
| **Pruebas** | Manuales E2E | Unitarias + Integración + Contratos |
| **CI/CD** | docker-compose up | GitHub Actions + Deploy automatizado |

---

## Recomendaciones de buenas prácticas

1. **Nombres de servicios**: Usar mayúsculas en `spring.application.name` para consistencia con Eureka (`CUSTOMER-SERVICE`, `PRODUCT-SERVICE`, etc.)
2. **DTOs separados de entidades**: Nunca exponer entidades JPA directamente en los endpoints
3. **Validación en el borde**: Validar DTOs de entrada con `@Valid` + `@NotBlank`, `@NotNull`, `@Positive`, etc.
4. **Manejo de errores centralizado**: Un solo `@RestControllerAdvice` por servicio que capture `MethodArgumentNotValidException`, `EntityNotFoundException`, `GenericException`
5. **Perfiles Spring**: `dev` → H2 + logs debug; `docker` → PostgreSQL + logs info
6. **Propiedades externalizadas**: Kafka topic, URLs de BD, puertos → todo en `application.yml` con valores por defecto
7. **Health checks**: `spring-boot-starter-actuator` con `/actuator/health` habilitado en todos los servicios
8. **Logs estructurados**: Usar `logback-spring.xml` con patrón JSON (LogstashEncoder) para entorno docker
9. **Evento Kafka**: Contrato JSON compartido documentado; considerar `@JsonTypeInfo` para versionado futuro
10. **Docker multi-stage**: Build con Maven JDK 17, runtime con JRE 17 Alpine (~100MB vs ~300MB)

---

## Posibles extensiones futuras

- Frontend básico (React/Angular) conectado a Gateway
- API Documentation con SpringDoc OpenAPI en cada servicio
- Agregar MongoDB para notification-service (historial de eventos)
- Migrar a schema registry con Avro para eventos tipados
- CI/CD con GitHub Actions: build → test → docker → deploy
- Despliegue en Kubernetes (minikube o cloud)
