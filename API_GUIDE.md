# API Guide

Base URL: `http://localhost:8089/api`

Gateway expone los servicios en el puerto **8089** del host (mapea a 8080 del contenedor). Todos los endpoints requieren `Content-Type: application/json`.

---

## Clientes — `/api/clientes`

### POST /api/clientes

Crear un nuevo cliente.

**Body:**

```json
{
  "nombre": "Juan Pérez",
  "correo": "juan@example.com"
}
```

**Respuesta 201:**

```json
{
  "id": 1,
  "nombre": "Juan Pérez",
  "correo": "juan@example.com"
}
```

**Errores:**
- `400` — nombre vacío, correo inválido o vacío
- `409` — ya existe un cliente con ese correo

---

### GET /api/clientes

Listar clientes con paginación.

**Parámetros query (opcionales):**

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `page` | `0` | Número de página |
| `size` | `20` | Resultados por página |
| `sort` | `id` | Campo de ordenamiento |

**Ejemplo:** `GET /api/clientes?page=0&size=10&sort=id`

**Respuesta 200:**

```json
{
  "content": [
    {
      "id": 1,
      "nombre": "Juan Pérez",
      "correo": "juan@example.com"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### GET /api/clientes/{id}

Obtener un cliente por ID.

**Respuesta 200:** Misma estructura que POST.

**Errores:**
- `404` — cliente no encontrado

---

### PUT /api/clientes/{id}

Actualizar un cliente existente.

**Body:** Misma estructura que POST.

**Respuesta 200:** Cliente actualizado.

**Errores:**
- `400` — validación de campos
- `404` — cliente no encontrado
- `409` — el nuevo correo ya está en uso por otro cliente

---

### DELETE /api/clientes/{id}

Eliminar un cliente.

**Respuesta 204:** Sin contenido.

**Errores:**
- `404` — cliente no encontrado

---

## Productos — `/api/productos`

### POST /api/productos

Crear un nuevo producto.

**Body:**

```json
{
  "nombre": "Laptop Gamer",
  "precio": 1500.00,
  "stock": 10
}
```

**Respuesta 201:**

```json
{
  "id": 1,
  "nombre": "Laptop Gamer",
  "precio": 1500.00,
  "stock": 10
}
```

**Errores:**
- `400` — nombre vacío, precio ≤ 0, stock negativo
- `409` — ya existe un producto con ese nombre

---

### GET /api/productos

Listar productos con paginación.

**Parámetros:** Mismos que `GET /api/clientes` (`page`, `size`, `sort`).

**Respuesta 200:**

```json
{
  "content": [
    {
      "id": 1,
      "nombre": "Laptop Gamer",
      "precio": 1500.00,
      "stock": 10
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### GET /api/productos/{id}

Obtener un producto por ID.

**Errores:**
- `404` — producto no encontrado

---

### PUT /api/productos/{id}

Actualizar un producto existente.

**Body:** Misma estructura que POST.

**Errores:**
- `404` — producto no encontrado
- `409` — el nuevo nombre ya está en uso

---

### DELETE /api/productos/{id}

Eliminar un producto.

**Respuesta 204.**

**Errores:**
- `404` — producto no encontrado

---

## Pedidos — `/api/pedidos`

### POST /api/pedidos

Crear un pedido. Valida que el cliente exista, el producto exista y haya stock suficiente.

**Body:**

```json
{
  "clienteId": 1,
  "productoId": 1,
  "cantidad": 2
}
```

**Respuesta 201:**

```json
{
  "id": 1,
  "clienteId": 1,
  "productoId": 1,
  "cantidad": 2,
  "total": 3000.00,
  "estado": "CREADO",
  "fechaCreacion": "2026-05-23T10:00:00"
}
```

Notas:
- `total` = precio del producto × cantidad (se obtiene del product-service)
- `estado` siempre es `"CREADO"`
- `fechaCreacion` se asigna automáticamente
- Al crear el pedido se publica un evento en Kafka topic `order-topic`

**Errores:**
- `400` — campos inválidos, cantidad ≤ 0, stock insuficiente
- `404` — cliente no encontrado, producto no encontrado

---

### GET /api/pedidos

Listar pedidos con paginación.

**Parámetros:** Mismos que `GET /api/clientes` (`page`, `size`, `sort`).

**Respuesta 200:**

```json
{
  "content": [
    {
      "id": 1,
      "clienteId": 1,
      "productoId": 1,
      "cantidad": 2,
      "total": 3000.00,
      "estado": "CREADO",
      "fechaCreacion": "2026-05-23T10:00:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### GET /api/pedidos/{id}

Obtener un pedido por ID.

**Errores:**
- `404` — pedido no encontrado

---

### DELETE /api/pedidos/{id}

Eliminar un pedido.

**Respuesta 204.**

**Errores:**
- `404` — pedido no encontrado

---

## Flujo de prueba completo

```bash
# 1. Crear cliente
curl -s -X POST http://localhost:8089/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ana López","correo":"ana@example.com"}'

# 2. Crear producto
curl -s -X POST http://localhost:8089/api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Monitor 4K","precio":800.00,"stock":5}'

# 3. Crear pedido (reemplazar IDs con los obtenidos arriba)
curl -s -X POST http://localhost:8089/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":1,"productoId":1,"cantidad":2}'

# 4. Verificar que el consumer procesó el evento
docker compose logs notification-service --tail=20
```

---

## Códigos de error globales

Todos los errores siguen esta estructura:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Cliente no encontrado con id: 9999",
  "path": "/api/clientes/9999",
  "timestamp": "2026-05-23T10:00:00"
}
```

Errores de validación incluyen `fieldErrors`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación en los campos enviados",
  "path": "/api/clientes",
  "timestamp": "2026-05-23T10:00:00",
  "fieldErrors": [
    { "field": "correo", "message": "El formato del correo no es válido" }
  ]
}
```

| Código | Significado | Ejemplo de mensaje |
|--------|-------------|--------------------|
| 400 | Validación de campos o negocio | `"Stock insuficiente. Disponible: 5, solicitado: 50"` |
| 400 | Formato inválido | `"Formato de solicitud inválido"` |
| 404 | Recurso no encontrado | `"Producto no encontrado con id: 99"` |
| 409 | Conflicto (duplicado) | `"Ya existe un cliente con el correo: ..."` |
| 500 | Error interno del servidor | `"Error interno del servidor"` |

---

## Infraestructura

| URL | Propósito |
|-----|-----------|
| `http://localhost:8761` | Dashboard Eureka (servicios registrados) |
| `http://localhost:8090` | Kafka UI (topics, mensajes, consumers, offsets) |
| `http://localhost:8089/actuator/health` | Health check del Gateway |
| `http://localhost:8081/actuator/health` | Health check customer-service (directo) |
| `http://localhost:8082/actuator/health` | Health check product-service (directo) |
| `http://localhost:8083/actuator/health` | Health check order-service (directo) |
| `http://localhost:8084/actuator/health` | Health check notification-service (directo) |
