# Gestión de Congresos - Backend

Backend desarrollado con **Spring Boot** para el sistema de gestión de congresos de la Universidad Nacional de Lanús.

---

# Tecnologías utilizadas

* Java 21
* Spring Boot 3.5.16
* Spring Web
* Spring Data JPA
* Spring Security
* MySQL 8
* Maven
* Lombok
* Docker (opcional)

---

# Requisitos

Antes de ejecutar el proyecto es necesario tener instalado:

* JDK 21
* Maven 3.9 o superior
* MySQL 8.x (si se ejecuta sin Docker)

o bien

* Docker Desktop
* Docker Compose

---

# Clonar el proyecto

```bash
git clone <URL_DEL_REPOSITORIO>
cd gestion-congresos-backend
```

---

# Variables de entorno

El proyecto utiliza variables de entorno para la conexión con la base de datos.

Ejemplo:

```properties
DB_URL=jdbc:mysql://localhost:3306/gestion_congresos
DB_USERNAME=gestion_congresos_user
DB_PASSWORD=********
```

Estas variables pueden definirse mediante un archivo `.env` o desde el sistema operativo.

> **Importante:** el archivo `.env` no debe subirse al repositorio.

---

# Ejecutar el proyecto (modo local)

Desde la raíz del proyecto:

```bash
mvn spring-boot:run
```

La aplicación quedará disponible en:

```
http://localhost:8080
```

---

# Ejecutar utilizando Docker

El backend se encuentra preparado para ejecutarse mediante Docker Compose desde el repositorio:

```
gestion-congresos-deploy
```

Desde dicho repositorio:

```bash
docker compose up --build
```

---

# Arquitectura Actual



---

# API REST

Actualmente el proyecto expone endpoints REST para realizar pruebas y operaciones sobre la base de datos.

Ejemplo:

```
GET /api/users/test
```

---

# Base de datos

El proyecto utiliza MySQL.

Hibernate administra el esquema mediante la propiedad:

```properties
spring.jpa.hibernate.ddl-auto=create
```

Durante el desarrollo esta propiedad puede cambiarse por:

```properties
spring.jpa.hibernate.ddl-auto=update
```

según la necesidad del equipo.

---

# Seguridad

Actualmente Spring Security se encuentra configurado para permitir todas las solicitudes con el objetivo de facilitar el desarrollo.

En futuras etapas se implementará autenticación y autorización mediante usuarios y roles.

---

# Estructura Git

El proyecto utiliza una estrategia de branching basada en un **Git Flow simplificado**. La estrategia se encuentra explicada detalladamente en el archivo **branching-strategy.md** dentro de la carpeta **docs**.

```
main
│
└── develop
      │
      ├── feature/...
      ├── bugfix/...
      └── hotfix/...
```

El desarrollo diario debe realizarse sobre ramas **feature**, integrándose posteriormente a **develop** mediante Pull Request.

---

# Integración con el Frontend

El frontend desarrollado en React consume la API REST expuesta por este backend.

Comunicación:

```
React
    │
HTTP REST
    │
Spring Boot
    │
JPA / Hibernate
    │
MySQL
```

---

# Autores

Trabajo Final Integrador

Licenciatura en Sistemas Informáticos

Universidad Nacional de Lanús
