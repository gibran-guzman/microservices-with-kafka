# microservices-with-kafka

Mini arquitectura empresarial de microservicios con Spring Boot 3.2.5, Spring Cloud 2023.0.1, Netflix Eureka, Spring Cloud Gateway, Apache Kafka y Docker Compose.

El proyecto implementa un flujo de compra asíncrono: los pedidos se crean con validación entre servicios (cliente existe, producto existe, stock suficiente) y se notifica vía eventos Kafka. Cada servicio tiene su propia base de datos PostgreSQL con persistencia por volumen.

---

## Requisitos previos

| Herramienta | Versión mínima |
|-------------|----------------|
| Java | 17+ |
| Maven | 3.9+ |
| Docker | 24+ |
| Docker Compose | 2.24+ |
| jq | 1.6+ (opcional, para los ejemplos) |

---

## Instalación

```bash
# 1. Clonar y compilar todos los módulos
git clone <repo-url>
cd microservices-with-kafka
mvn clean package -DskipTests

# 2. Opcional: personalizar credenciales (usa valores por defecto si se omite)
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres

# 3. Levantar toda la infraestructura (12 contenedores)
docker compose up --build -d

# 4. Verificar que todos los servicios están healthy
docker compose ps

# 5. Confirmar que Eureka y Kafka UI responden
curl -s http://localhost:8761/actuator/health | jq .
curl -s http://localhost:8090 | head -1
```

Tiempo estimado: 2-5 minutos (depende de la descarga de imágenes Docker).

---

## Ejemplo de uso rápido

```bash
# Crear cliente
CLIENTE=$(curl -s -X POST http://localhost:8088/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Juan Pérez","correo":"juan@example.com"}' | jq -r '.id')
echo "Cliente ID: $CLIENTE"

# Crear producto
PRODUCTO=$(curl -s -X POST http://localhost:8088/api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Laptop Gamer","precio":1500.00,"stock":10}' | jq -r '.id')
echo "Producto ID: $PRODUCTO"

# Crear pedido (dispara evento Kafka)
PEDIDO=$(curl -s -X POST http://localhost:8088/api/pedidos \
  -H "Content-Type: application/json" \
  -d "{\"clienteId\":$CLIENTE,\"productoId\":$PRODUCTO,\"cantidad\":2}" | jq .)
echo "Pedido creado: $PEDIDO"

# Ver la notificación en el consumer
docker compose logs notification-service --tail=20

# Explorar eventos en Kafka UI
echo "Abrir http://localhost:8090 en el navegador"
```

---

## Estructura del proyecto

```
microservices-with-kafka/
├── common-lib/                          # Librería compartida
│   └── src/main/java/.../exception/     # ErrorResponse, GlobalExceptionHandler,
│                                        # ResourceNotFoundException, DuplicateResourceException
├── eureka-server/                       # Service Discovery (Netflix Eureka, puerto 8761)
├── gateway-server/                      # API Gateway (Spring Cloud Gateway, puerto 8088)
│   └── src/main/java/.../config/        # CorsConfig
├── customer-service/                    # CRUD de clientes (puerto 8081)
│   └── src/main/java/.../customer/      # controller/ service/ repository/ entity/ dto/ mapper/
├── product-service/                     # CRUD de productos con stock (puerto 8082)
│   └── src/main/java/.../product/       # controller/ service/ repository/ entity/ dto/ mapper/
├── order-service/                       # Pedidos con validación + Kafka producer (puerto 8083)
│   ├── src/main/java/.../order/
│   │   ├── controller/ service/ repository/ entity/ dto/ mapper/
│   │   ├── event/                       # OrderEvent, OrderEventProducer
│   │   └── config/                      # RestClientConfig (inter-service calls)
├── notification-service/               # Consumer Kafka con idempotencia (puerto 8084)
│   ├── src/main/java/.../notification/
│   │   ├── event/                       # OrderEventConsumer, OrderEvent
│   │   ├── config/                      # KafkaConsumerConfig (error handler, DLT)
│   │   ├── entity/                      # ProcessedEvent (idempotencia)
│   │   └── repository/                  # ProcessedEventRepository
├── docker-compose.yml                   # 12 contenedores (Zookeeper, Kafka, Kafka UI,
│                                        # 3×PostgreSQL, 6 microservicios)
├── postman_collection.json              # 22 requests organizadas para probar todo
└── ARCHITECTURE.md                      # Diagramas Mermaid del flujo completo
```

---

## Variables de entorno

| Variable | Descripción | Default | Obligatoria |
|----------|-------------|---------|-------------|
| `POSTGRES_USER` | Usuario PostgreSQL para todas las bases de datos | `postgres` | No |
| `POSTGRES_PASSWORD` | Contraseña PostgreSQL | `postgres` | No |

El resto de variables (Kafka, Eureka, datasources) están configuradas con valores por defecto en `application.yml` y `docker-compose.yml`.

---

## Tests

```bash
mvn test
```

26 tests unitarios distribuidos:
- CustomerServiceTest: 9 tests
- ProductServiceTest: 9 tests
- OrderServiceTest: 8 tests

Cobertura: capa de servicio con Mockito, incluyendo casos de éxito, validación, recursos no encontrados, duplicados y stock insuficiente.

---

## Cómo contribuir

1. Fork del repositorio
2. Crear rama: `git checkout -b feature/nombre-cambio`
3. Hacer cambios y verificar que `mvn test` pasa
4. Commit convencional: `feat:|fix:|refactor:|docs:|test:|chore:`
5. Abrir Pull Request describiendo el cambio y su motivación

---

## Puertos

| Servicio | Puerto (host → container) |
|----------|---------------------------|
| Gateway | 8088 → 8080 |
| Eureka Server | 8761 |
| customer-service | 8081 |
| product-service | 8082 |
| order-service | 8083 |
| notification-service | 8084 |
| Kafka | 9092 |
| Zookeeper | 2181 |
| Kafka UI | 8090 → 8080 |
| customer-db (PostgreSQL) | 5433 → 5432 |
| product-db (PostgreSQL) | 5434 → 5432 |
| order-db (PostgreSQL) | 5435 → 5432 |
