# Catálogo y decisiones de coautoría

Entrega backend de FE-14: commit directo en `develop`, autorizado por el equipo. Contrato y flujo frontend detallados en la especificación `docs/FE-paper-coauthorship.md` del repositorio frontend.

El catálogo se implementa en `PaperAuthorService#getAvailableForCoauthorship` y `PaperAuthorServiceImpl`, junto con las solicitudes y decisiones de autoría. No se agrega un servicio de catálogo independiente; `CoauthorshipPolicy` reúne las validaciones reutilizables.

## API

`GET /api/papers/congresses/{congressCode}?forCoauthorship=true` agrega un modo de consulta sin cambiar el comportamiento anterior cuando el parámetro no está o es false. Swagger documenta ambas páginas de respuesta.

La página de coautoría solo incluye código, título, área, cantidad de autores aceptados y máximo de autores. `search` busca coincidencias parciales en código/título y `thematicArea` conserva la coincidencia exacta existente. Se fuerza orden por código y un máximo de 100 filas por página. Los filtros administrativos no permiten eludir los criterios de elegibilidad.

Se valida cuenta habilitada, participación activa con institución y rol LISTENER/EXPOSITOR, excluyendo superadministradores. La consulta exige NOT_SUBMITTED, cupo disponible y ausencia de relación del solicitante; los totales se calculan después de filtrar. Los pendientes no ocupan cupo y un máximo nulo no limita autores.

El período de presentación y la habilitación del congreso se validan con `DateUtils.now()` de Argentina, incluyendo apertura y cierre. El catálogo cerrado devuelve página vacía; crear/resolver solicitudes fuera de período devuelve un error de validación.

Los endpoints existentes POST request y PATCH accept/reject conservan sus respuestas. Se revalidan identidad, permisos, inscripción, institución, estado y capacidad. El listado de pendientes se puede consultar fuera del período. Se mantienen promoción a expositor, eliminación del rechazo y notificaciones por email.

Se usa un bloqueo PESSIMISTIC_WRITE sobre el trabajo para coordinar solicitudes, decisiones, envío y mutaciones de autoría. No requiere migraciones. Publicar backend antes del frontend.

## Pruebas

Las pruebas nuevas se conservan únicamente en el workspace y están excluidas del commit por pedido del equipo, mediante entradas específicas en `.git/info/exclude`. No se modifica el `.gitignore` compartido ni se excluyen pruebas existentes de otros desarrolladores. Los siguientes comandos requieren esos archivos locales:

```sh
mvn -Dtest=PaperControllerCoauthorshipTest,PaperAuthorServiceCoauthorshipTest,CoauthorshipPolicyTest test
```

La prueba `CoauthorshipRepositoryTest` solo se habilita si `C4T_TEST_DB_URL` apunta exactamente a `jdbc:mysql://127.0.0.1:3308/c4t_coauthorship_test` (con parámetros opcionales). Crea y elimina el esquema; nunca debe apuntarse a datos del proyecto. Utiliza usuario root y contraseña local de pruebas `c4t-test-only`.

```sh
docker run -d --name c4t-coauthorship-test -p 127.0.0.1:3308:3306 -e MYSQL_ROOT_PASSWORD=c4t-test-only -e MYSQL_DATABASE=c4t_coauthorship_test --tmpfs /var/lib/mysql mysql:8.4
# Configurar C4T_TEST_DB_URL=jdbc:mysql://127.0.0.1:3308/c4t_coauthorship_test?allowPublicKeyRetrieval=true&useSSL=false
mvn -Dtest=*Coauthorship*Test,*ParticipantHomeTest test
```

Incluye consultas reales de paginación/cupo, rechazo con nueva solicitud, estados desactualizados y dos aceptaciones compitiendo por el último lugar. Los servicios de correo se simulan en estas pruebas; no se usan cuentas ni datos reales.

## Resultados — 25/11/2026

- 20 pruebas focalizadas aprobadas y JAR generado mediante Maven.
- Tras concentrar el catálogo en `PaperAuthorServiceImpl`, se recompiló el backend y pasaron las 5 pruebas locales focalizadas del controlador, catálogo y política de coautoría.
- Swagger y ambos modos del listado verificados sobre una instancia aislada de la API en 8081.
- Flujo completo solicitante/responsable probado en Chrome contra esa API y MySQL temporal: sin respuestas simuladas, con usuarios ficticios y SMTP local.
- Cierre del período comprobado usando fecha argentina; las decisiones pasan a devolver 400 y la consulta de pendientes continúa disponible.
- La base de pruebas y la instancia de API son independientes de los contenedores habituales. Se preservó el archivo local `application.properties` del usuario. Las pruebas nuevas quedan fuera del commit, por pedido del equipo.
