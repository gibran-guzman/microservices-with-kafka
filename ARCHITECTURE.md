# Diagrama de Arquitectura

## Flujo de comunicación entre servicios

```mermaid
graph TB
    subgraph Cliente["🌐 Cliente Externo"]
        HTTP[("HTTP :8088")]
    end

    subgraph Discovery["🔍 Service Discovery"]
        EUREKA[("Eureka Server<br/>:8761")]
    end

    subgraph Gateway["🚪 API Gateway"]
        GW[("Gateway Server<br/>:8088 → :8080")]
    end

    subgraph Clientes["👤 Customer Service"]
        CS_API["Controller /clientes"]
        CS_SVC["Service"]
        CS_REPO[("PostgreSQL<br/>customerdb<br/>:5433")]
    end

    subgraph Productos["📦 Product Service"]
        PS_API["Controller /productos"]
        PS_SVC["Service"]
        PS_REPO[("PostgreSQL<br/>productdb<br/>:5434")]
    end

    subgraph Pedidos["📋 Order Service"]
        OS_API["Controller /pedidos"]
        OS_SVC["Service"]
        OS_REPO[("PostgreSQL<br/>orderdb<br/>:5435")]
        OS_KAFKA["OrderEventProducer"]
        OS_REST["RestClient<br/>(@LoadBalanced)"]
    end

    subgraph Kafka["📨 Mensajería"]
        ZK[("Zookeeper<br/>:2181")]
        KAFKA{{"Kafka<br/>:9092<br/>topic: order-topic<br/>DLT: order-topic-dlt"}}
    end

    subgraph Notificaciones["🔔 Notification Service"]
        NS_CONSUMER["OrderEventConsumer<br/>@KafkaListener"]
        NS_IDEM[("H2 In-Memory<br/>processed_events")]
    end

    %% Conexiones HTTP
    HTTP -->|"/api/clientes/**"| GW
    HTTP -->|"/api/productos/**"| GW
    HTTP -->|"/api/pedidos/**"| GW

    GW -->|"lb://CUSTOMER-SERVICE"| CS_API
    GW -->|"lb://PRODUCT-SERVICE"| PS_API
    GW -->|"lb://ORDER-SERVICE"| OS_API

    CS_API --> CS_SVC --> CS_REPO
    PS_API --> PS_SVC --> PS_REPO

    OS_API --> OS_SVC --> OS_REPO
    OS_SVC -->|"① GET /clientes/{id}"| OS_REST --> CS_SVC
    OS_SVC -->|"② GET /productos/{id}"| OS_REST --> PS_SVC
    OS_SVC -->|"③ send() síncrono"| OS_KAFKA

    OS_KAFKA -->|"orderEvent"| KAFKA
    KAFKA -->|"consumer"| NS_CONSUMER
    NS_CONSUMER --> NS_IDEM

    %% Discovery
    EUREKA -.->|"Registro"| CS_SVC
    EUREKA -.->|"Registro"| PS_SVC
    EUREKA -.->|"Registro"| OS_SVC
    EUREKA -.->|"Registro"| NS_CONSUMER
    EUREKA -.->|"Registro"| GW

    KAFKA -.-> ZK
```

## Flujo de creación de un pedido (paso a paso)

```mermaid
sequenceDiagram
    participant C as Cliente
    participant GW as Gateway (:8088)
    participant OS as Order Service
    participant CS as Customer Service
    participant PS as Product Service
    participant DB as Order DB
    participant KP as Kafka Producer
    participant K as Kafka (order-topic)
    participant NS as Notification Service

    C->>GW: POST /api/pedidos<br/>{clienteId, productoId, cantidad}
    GW->>OS: Reenvía a ORDER-SERVICE

    OS->>CS: GET /clientes/{clienteId}
    CS-->>OS: 200 OK (cliente existe)

    OS->>PS: GET /productos/{productoId}
    PS-->>OS: 200 OK + {precio, stock}

    alt stock >= cantidad
        OS->>OS: Calcula total = precio × cantidad
        OS->>OS: Asigna estado = "CREADO"
        OS->>DB: INSERT INTO orders
        DB-->>OS: Order guardada

        OS->>KP: send(orderEvent) [síncrono, timeout 10s]
        KP->>K: Publica en order-topic
        K-->>KP: ACK
        KP-->>OS: Éxito

        OS-->>GW: 201 Created + OrderResponse
        GW-->>C: 📦 Pedido creado

        K->>NS: Consumer recibe evento
        NS->>NS: Verifica idempotencia en H2
        NS->>NS: 🔔 LOG: Pedido recibido

    else stock < cantidad
        OS-->>GW: 400 Bad Request<br/>"Stock insuficiente"
        GW-->>C: ❌ Error
    end
```

## Flujo de validación de datos duplicados

```mermaid
sequenceDiagram
    participant C as Cliente
    participant SVC as Service
    participant DB as PostgreSQL

    C->>SVC: POST /clientes {correo: "x@x.com"}

    SVC->>DB: INSERT INTO customers
    DB-->>SVC: ERROR: unique constraint violation

    SVC->>SVC: Captura DataIntegrityViolationException
    SVC-->>C: 409 Conflict<br/>"Ya existe un cliente con el correo: x@x.com"
```

## Consumo resiliente de Kafka (dead-letter + idempotencia)

```mermaid
sequenceDiagram
    participant K as Kafka (order-topic)
    participant NS as Notification Service
    participant H2 as H2 (processed_events)
    participant DLT as Kafka (order-topic-dlt)

    K->>NS: Evento (primer intento)
    NS->>NS: ¿Evento nulo?
    NS->>NS: ¿Ya procesado? (existsByEventKey)

    alt Ya procesado
        NS-->>K: Commit, ignorar
    else No procesado
        NS->>H2: INSERT processed_events
        NS->>NS: 📝 Procesar lógica de negocio
        NS-->>K: Commit exitoso
    end

    alt Error de procesamiento
        NS->>NS: Retry (3 veces, 1s backoff)
        rect rgb(255, 200, 200)
            Note over NS,DLT: Todos los reintentos fallaron
            NS->>DLT: Publicar en DLT
            NS-->>K: Commit (mensaje movido a DLT)
        end
    end
```

## Topología de Docker

```mermaid
graph TB
    subgraph Docker["🐳 Docker Compose (11 contenedores)"]
        NET[microservices-network]

        subgraph Infra["Infraestructura"]
            ZK[Zookeeper 7.6.0<br/>:2181]
            KAFKA[Kafka 7.6.0<br/>:9092]
        end

        subgraph DBs["Bases de Datos"]
            CDB[("customer-db<br/>PostgreSQL 16<br/>:5433 ← :5432<br/>vol: customer-db-data")]
            PDB[("product-db<br/>PostgreSQL 16<br/>:5434 ← :5432<br/>vol: product-db-data")]
            ODB[("order-db<br/>PostgreSQL 16<br/>:5435 ← :5432<br/>vol: order-db-data")]
        end

        subgraph Services["Microservicios (Spring Boot 3.2.5)"]
            EUREKA[Eureka Server<br/>:8761]
            GW[Gateway Server<br/>:8088 ← :8080]
            CS[Customer Service<br/>:8081]
            PS[Product Service<br/>:8082]
            OS[Order Service<br/>:8083]
            NS[Notification Service<br/>:8084]
        end
    end

    CDB -.-> NET
    PDB -.-> NET
    ODB -.-> NET
    EUREKA -.-> NET
    GW -.-> NET
    CS -.-> NET
    PS -.-> NET
    OS -.-> NET
    NS -.-> NET
    ZK -.-> NET
    KAFKA -.-> NET
```

## Endpoints disponibles

| Método | Ruta | Descripción | Servicio |
|--------|------|-------------|----------|
| POST | `/api/clientes` | Crear cliente | Customer |
| GET | `/api/clientes` | Listar clientes (paginado) | Customer |
| GET | `/api/clientes/{id}` | Obtener cliente por ID | Customer |
| PUT | `/api/clientes/{id}` | Actualizar cliente | Customer |
| DELETE | `/api/clientes/{id}` | Eliminar cliente | Customer |
| POST | `/api/productos` | Crear producto | Product |
| GET | `/api/productos` | Listar productos (paginado) | Product |
| GET | `/api/productos/{id}` | Obtener producto por ID | Product |
| PUT | `/api/productos/{id}` | Actualizar producto | Product |
| DELETE | `/api/productos/{id}` | Eliminar producto | Product |
| POST | `/api/pedidos` | Crear pedido (valida cliente, producto y stock) | Order |
| GET | `/api/pedidos` | Listar pedidos (paginado) | Order |
| GET | `/api/pedidos/{id}` | Obtener pedido por ID | Order |
| DELETE | `/api/pedidos/{id}` | Eliminar pedido | Order |

## Patrones implementados

| Patrón | Implementación |
|--------|----------------|
| **API Gateway** | Spring Cloud Gateway con Eureka discovery (`lb://`) |
| **Service Discovery** | Netflix Eureka (todos los servicios se registran) |
| **Database per Service** | Cada microservicio tiene su propia BD PostgreSQL |
| **Event-Driven Async** | Kafka producer en order-service, consumer en notification-service |
| **Dead Letter Topic** | `order-topic-dlt` para mensajes fallidos después de 3 reintentos |
| **Idempotent Consumer** | Tabla `processed_events` (H2) para filtrar duplicados |
| **Transactional Outbox (simplificado)** | Kafka send síncrono dentro de `@Transactional` de JPA |
| **Saga Coreografía** | order-service orquesta validación vía RestClient a customer/product |
| **Pagination** | Spring Data `Pageable` en todos los endpoints `findAll` |
| **Global Exception Handler** | `@RestControllerAdvice` compartido vía common-lib |
| **CORS** | Gateway con `CorsWebFilter` permitiendo todos los orígenes |

## Cómo probar el flujo completo

```bash
# 1. Asegurar que los JARs están compilados
mvn clean package -DskipTests

# 2. Levantar toda la infraestructura
docker compose up --build -d

# 3. Verificar que todos los contenedores están healthy
docker compose ps

# 4. Importar postman_collection.json en Postman
# 5. Ejecutar en orden:
#    - Crear cliente → Crear producto → Crear pedido
# 6. Verificar logs del notification-service:
docker compose logs notification-service

# 7. Para ver el evento publicado en Kafka (opcional):
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-topic \
  --from-beginning

# 8. Para ver mensajes fallidos en el DLT:
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-topic-dlt \
  --from-beginning
```
