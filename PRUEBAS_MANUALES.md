# Revisión antes de la exposición

Ejecuta estos casos en una instalación local de demostración con fichas ficticias. Guarda capturas de los resultados para tu presentación. Esta lista define resultados esperados; no declara que el despliegue ya fue probado.

| Caso | Acción | Resultado esperado |
|---|---|---|
| Acceso | Usar una contraseña incorrecta. | Rechaza el acceso sin revelar si existe la cuenta. |
| Roles | Crear un DIRECTOR y abrir la URL de Estudiantes con esa cuenta. | Responde sin permiso; puede consultar reportes. |
| Cuenta inactiva | Desactivar una cuenta de matrícula que ya tiene sesión abierta. | En la siguiente solicitud deja de tener acceso. |
| Documento duplicado | Crear dos estudiantes con el mismo tipo y número de documento. | Rechaza el segundo registro y conserva el primero. |
| Coincidencia probable | Registrar nombres, apellidos y nacimiento iguales a otra ficha. | Solicita revisar la coincidencia; permite confirmar que es otra persona. |
| Solicitud duplicada | Crear otra matrícula pendiente para el mismo estudiante y periodo. | Solicita revisar la matrícula existente. |
| Identidad pendiente | Intentar activar un estudiante sin identidad verificada. | Mantiene la matrícula pendiente e informa la causa. |
| Responsable | Intentar activar sin asignar un apoderado vinculado y activo. | Mantiene la matrícula pendiente. |
| Documentos | Dejar un requisito obligatorio sin verificar y activar. | Rechaza la activación. |
| Periodo cerrado | Cerrar un periodo y activar una solicitud existente. | Rechaza la operación sin alterar la vacante. |
| Capacidad | Configurar una sección de capacidad 1 y activar dos estudiantes diferentes. | Solo la primera activación procede. |
| Concurrencia | Con dos sesiones, activar al mismo tiempo dos solicitudes para la última vacante. | Solo una queda activa; nunca supera la capacidad. |
| Doble matrícula activa | Tras anular/retirar, crear otra solicitud; verificar que el índice impide dos activas si se intenta una operación duplicada. | Como máximo una matrícula activa del estudiante por periodo. |
| Cambio sin vacante | Cambiar una matrícula activa hacia una sección llena. | Conserva la sección de origen y su vacante ocupada. |
| Cambio válido | Cambiar de A a B dentro del mismo grado. | A libera una vacante y B ocupa una; aparece el movimiento. |
| Retiro | Retirar una matrícula activa con ADMIN y motivo. | Estado RETIRADA; libera vacante y conserva historial. |
| Anulación | Anular una matrícula pendiente o activa. | Estado ANULADA; se conserva el registro. |
| Capacidad inferior | Reducir la capacidad por debajo de las matrículas activas. | Rechaza el cambio de configuración. |
| Ficha con activa | Desactivar a un estudiante o su responsable mientras tenga matrícula activa. | Rechaza la desactivación. |
| Transacción | Provocar un fallo de conexión durante una operación, usando solo el entorno de pruebas. | Ninguna modificación parcial confirmada; revisar base e historial. |
| Historial | Revisar una activación, un cambio y una anulación. | Cada movimiento conserva usuario, fecha, estados, secciones y motivo. |
| Bitácora | Revisar una actualización de ficha con ADMIN. | Muestra valores anteriores y nuevos; no muestra contraseñas originales. |
| Formulario expirado | Cambiar de sesión e intentar enviar un formulario anterior. | Rechaza el token y pide recargar. |
| Reportes | Filtrar un periodo e imprimir. | El reporte muestra periodo, fecha, capacidad, activas y disponibles. |

Para observar la capacidad y las matrículas activas en Workbench:

```sql
SELECT * FROM matricula_escolar.vw_vacantes;
SELECT estudiante_id, periodo_id, COUNT(*)
FROM matricula_escolar.matriculas
WHERE estado = 'ACTIVA'
GROUP BY estudiante_id, periodo_id
HAVING COUNT(*) > 1;
```

La segunda consulta debe devolver cero filas. Todas las secciones deben cumplir `ocupadas <= capacidad`.
