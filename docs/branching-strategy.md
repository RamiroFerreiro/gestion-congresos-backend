# Estrategia de Branching - Gestión de Congresos Backend

## Introducción

Este documento define la estrategia de trabajo con ramas (_branching
strategy_) utilizada en el proyecto **Gestión de Congresos Backend**.

El objetivo es mantener un flujo de trabajo ordenado, permitir el
desarrollo paralelo entre integrantes del equipo y reducir conflictos al
momento de integrar cambios.

La estrategia elegida está basada en un **Git Flow simplificado**,
adaptado a las necesidades del proyecto.

---

# Estructura de ramas

```text
main
  ↑
develop
  ↑
feature/*
```

## Rama `main`

- Contiene únicamente código estable.
- No se desarrolla directamente sobre ella.
- Recibe cambios desde `develop` **únicamente a través de Pull Requests**.

## Rama `develop`

- Rama principal de integración y desarrollo.
- Base para crear nuevas `feature/*`.
- Recibe las integraciones de las features de manera local.
- Debe mantenerse siempre compilable.

## Ramas `feature/*`

Ejemplos:

- `feature/auth`
- `feature/usuarios`
- `feature/trabajos`
- `feature/evaluaciones`
- `feature/project-structure`

Cada rama se crea desde `develop` y se integra nuevamente a ella de forma local una vez finalizada la tarea.

# Flujo de trabajo

## 1. Actualizar develop local

```bash
git checkout develop
git pull origin develop
```

## 2. Crear una feature

```bash
git checkout -b feature/nombre-funcionalidad
```

## 3. Trabajar y confirmar cambios

```bash
git add .
git commit -m "Descripción del cambio"
```

## 4. Integrar la feature en develop (Local)

Una vez que la funcionalidad esté lista y probada, regresa a develop, descarga posibles cambios de otros compañeros e integra tu rama:

```bash
# 1. Volver a develop y actualizarlo
git checkout develop
git pull origin develop

# 2. Fusionar la feature en develop
git merge feature/nombre-funcionalidad

# 3. Subir los cambios integrados al repositorio remoto
git push origin develop
```

## Flujo de trabajo para Producción (Paso a `main`)

Para pasar los cambios estables de `develop` a `main`, sí se utilizará un Pull Request (PR) en GitHub.

1. Sube la rama `develop` actualizada si no lo has hecho.

2. Abre un Pull Request desde `develop` hacia `main`.

3. Una vez revisado y aprobado por el equipo, se realiza el merge.

# Actualizar una feature en progreso con cambios de develop

Si estás trabajando en tu feature y necesitas traer cambios que otros subieron a `develop`:

Si no hay cambios sin commit:

```bash
git checkout develop
git pull origin develop

git checkout feature/nombre-funcionalidad
git merge develop
```

Si hay cambios sin commit:

```bash
git stash

git checkout develop
git pull origin develop

git checkout feature/nombre-funcionalidad
git merge develop

git stash pop
```

# Uso de git stash

Utilizar `git stash` para guardar temporalmente cambios sin commit y
`git stash pop` para recuperarlos posteriormente.

# Convenciones de commits

Ejemplos recomendados:

- `Implementa endpoint para crear usuarios`
- `Configura arquitectura base del backend`
- `Agrega validaciones para entidad Trabajo`

Evitar mensajes como:

- `Cambios`
- `Update`
- `Fix`

# Recomendaciones

- No hacer push directo a `main`. El paso a `main` es exclusivo por Pull Request.
- No desarrollar directamente sobre `develop`.
- Crear una rama por funcionalidad.
- Actualizar `develop` antes de comenzar una nueva tarea.
- Integrar tus ramas de feature a `develop` local de manera frecuente para evitar conflictos masivos.
- Mantener el proyecto compilable.

# Resumen visual

```text
                main
                 ↑ (Vía Pull Request)
             develop
          ↗     ↑     ↖ (Merges Locales)
 feature/auth feature/users feature/trabajos
```
