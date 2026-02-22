<img alt="header" src="https://capsule-render.vercel.app/api?type=waving&height=300&color=gradient&text=Bank-Core">

Proyecto backend basado en microservicios para gestión bancaria.

Incluye dos microservicios Spring Boot:
- `ms-customers`: registro, autenticación y gestión de perfil de clientes.
- `ms-accounts`: creación y consulta de cuentas bancarias asociadas a clientes.

## Arquitectura

- Lenguaje: Java 21
- Framework: Spring Boot 3.5.x
- Base de datos: PostgreSQL (una por microservicio)
- Autenticación: JWT
- Comunicación interna: OpenFeign (`ms-accounts` consume `ms-customers`)
- Documentación API: OpenAPI/Swagger
- Contenedores: Docker + Docker Compose

Estructura principal del repositorio:

```text
bank-core/
├── ms-customers/
├── ms-accounts/
├── docker-compose.yaml
└── README.md
```

## Microservicios

### 1) `ms-customers`
Responsabilidades principales:
- Registro de clientes (`/api/auth/register`)
- Login de clientes (`/api/auth/login`)
- Consulta de perfil autenticado (`/api/customers/me`)
- Actualización de perfil autenticado (`/api/customers/me`)

Configuración por defecto:
- Puerto: `8081`
- DB local: `jdbc:postgresql://localhost:5432/customers`

Swagger:
- `http://localhost:8081/swagger-ui/index.html`

### 2) `ms-accounts`
Responsabilidades principales:
- Crear cuentas bancarias para clientes (`/api/accounts`)
- Consultar cuenta por ID (`/api/accounts/{accountId}`)

Reglas de negocio destacadas:
- Máximo 3 cuentas por cliente
- Generación de IBAN único
- Validación de estado del cliente vía `ms-customers`

Configuración por defecto:
- Puerto: `8082`
- DB local: `jdbc:postgresql://localhost:5433/accounts`

Swagger:
- `http://localhost:8082/swagger-ui/index.html`

## Requisitos

- Java 21
- Maven 3.9+ (o usar `./mvnw` en cada microservicio)
- Docker y Docker Compose (opcional, recomendado)

## Instalación y ejecución

### Opción A: Docker Compose (recomendada)
Desde la raíz del proyecto:

```bash
docker compose up --build
```

Servicios levantados:
- `customers-db`: PostgreSQL en `localhost:5432`
- `accounts-db`: PostgreSQL en `localhost:5433`
- `ms-customers`: `localhost:8081`
- `ms-accounts`: `localhost:8082`

Para detener:

```bash
docker compose down
```

### Opción B: Ejecución local por microservicio
1. Levanta PostgreSQL para ambas bases (`customers` y `accounts`).
2. Verifica los `application.properties` de cada servicio.
3. Ejecuta cada microservicio por separado.

`ms-customers`:

```bash
cd ms-customers
./mvnw spring-boot:run
```

`ms-accounts`:

```bash
cd ms-accounts
./mvnw spring-boot:run
```

## Variables de configuración relevantes

En Docker Compose ya están definidas. Las principales son:

- `SPRING_DATASOURCE_URL`
- `SERVER_PORT`
- `JWT_SECRET`
- `JWT_EXPIRATION`
- `CUSTOMERS_SERVICE_URL` (en `ms-accounts`)
- `ACCOUNTS_SERVICE_URL` (en `ms-customers`)

## Pruebas

Ejecutar tests por microservicio:

```bash
cd ms-customers
./mvnw test
```

```bash
cd ms-accounts
./mvnw test
```

## Flujo básico de uso

1. Registrar cliente en `ms-customers`.
2. Iniciar sesión para obtener token JWT.
3. Usar token `Bearer` para operaciones autenticadas (`/api/customers/me`, `/api/accounts`, etc.).

## Contribuir

1. Crear una rama desde `main`:

```bash
git checkout -b feature/nombre-cambio
```

2. Hacer cambios pequeños y enfocados.
3. Ejecutar pruebas del/los microservicio(s) afectados.
4. Documentar cambios relevantes en este README si aplica.
5. Abrir Pull Request con objetivo del cambio, alcance técnico y evidencia de pruebas.

## Buenas prácticas sugeridas

- Mantener consistencia entre reglas de seguridad y endpoints públicos.
- No hardcodear secretos en código fuente para ambientes reales.
- Versionar cambios de contrato API cuando rompan compatibilidad.
- Priorizar cobertura de tests de servicio y controlador.

<img alt="footer" src="https://capsule-render.vercel.app/api?type=waving&height=300&color=gradient&section=footer">