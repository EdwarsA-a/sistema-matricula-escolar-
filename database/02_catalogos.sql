USE matricula_escolar;
INSERT INTO roles(nombre) VALUES ('ADMIN'),('MATRICULA'),('DIRECTOR');
INSERT INTO niveles(nombre) VALUES ('Primaria'),('Secundaria');
INSERT INTO grados(nivel_id,nombre)
SELECT id,'1ro' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'2do' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'3ro' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'4to' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'5to' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'6to' FROM niveles WHERE nombre='Primaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'1ro' FROM niveles WHERE nombre='Secundaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'2do' FROM niveles WHERE nombre='Secundaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'3ro' FROM niveles WHERE nombre='Secundaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'4to' FROM niveles WHERE nombre='Secundaria';
INSERT INTO grados(nivel_id,nombre)
SELECT id,'5to' FROM niveles WHERE nombre='Secundaria';
-- Datos de demostracion; ajustar a la institucion.
INSERT INTO periodos(codigo,fecha_inicio,fecha_fin,estado)
VALUES ('2026','2026-03-01','2026-12-31','ABIERTO');
INSERT INTO secciones(periodo_id,grado_id,nombre,capacidad)
SELECT p.id,g.id,'A',30 FROM periodos p JOIN grados g
JOIN niveles n ON n.id=g.nivel_id
WHERE p.codigo='2026' AND g.nombre='1ro' AND n.nombre='Primaria';
INSERT INTO secciones(periodo_id,grado_id,nombre,capacidad)
SELECT p.id,g.id,'B',30 FROM periodos p JOIN grados g
JOIN niveles n ON n.id=g.nivel_id
WHERE p.codigo='2026' AND g.nombre='1ro' AND n.nombre='Primaria';
INSERT INTO tipos_documento(nombre,obligatorio) VALUES
('Documento de identidad del estudiante - ejemplo',TRUE),
('Documento de identidad del apoderado - ejemplo',TRUE),
('Ficha de datos del estudiante - ejemplo',TRUE);
