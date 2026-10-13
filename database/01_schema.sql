CREATE DATABASE IF NOT EXISTS matricula_escolar
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE matricula_escolar;

CREATE TABLE roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE usuarios (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rol_id BIGINT NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (rol_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE niveles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(30) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE grados (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nivel_id BIGINT NOT NULL,
  nombre VARCHAR(30) NOT NULL,
  UNIQUE (nivel_id, nombre),
  FOREIGN KEY (nivel_id) REFERENCES niveles(id)
) ENGINE=InnoDB;

CREATE TABLE periodos (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  fecha_inicio DATE NOT NULL,
  fecha_fin DATE NOT NULL,
  estado ENUM('PLANIFICADO','ABIERTO','CERRADO')
    NOT NULL DEFAULT 'PLANIFICADO',
  CHECK (fecha_fin >= fecha_inicio)
) ENGINE=InnoDB;

CREATE TABLE secciones (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  periodo_id BIGINT NOT NULL,
  grado_id BIGINT NOT NULL,
  nombre VARCHAR(20) NOT NULL,
  capacidad INT NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (periodo_id, grado_id, nombre),
  UNIQUE (id, periodo_id),
  CHECK (capacidad > 0),
  FOREIGN KEY (periodo_id) REFERENCES periodos(id),
  FOREIGN KEY (grado_id) REFERENCES grados(id)
) ENGINE=InnoDB;

CREATE TABLE estudiantes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo VARCHAR(30) NOT NULL UNIQUE,
  tipo_documento VARCHAR(15) NOT NULL DEFAULT 'DNI',
  documento VARCHAR(20) NULL,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(100) NOT NULL,
  fecha_nacimiento DATE NOT NULL,
  direccion VARCHAR(200) NOT NULL,
  identidad_verificada BOOLEAN NOT NULL DEFAULT FALSE,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (tipo_documento, documento),
  INDEX ix_estudiante_nombre (apellidos, nombres)
) ENGINE=InnoDB;

CREATE TABLE apoderados (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tipo_documento VARCHAR(15) NOT NULL DEFAULT 'DNI',
  documento VARCHAR(20) NOT NULL,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(100) NOT NULL,
  telefono VARCHAR(25) NOT NULL,
  email VARCHAR(120) NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (tipo_documento, documento)
) ENGINE=InnoDB;

CREATE TABLE estudiante_apoderado (
  estudiante_id BIGINT NOT NULL,
  apoderado_id BIGINT NOT NULL,
  vinculo VARCHAR(30) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (estudiante_id, apoderado_id),
  FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id),
  FOREIGN KEY (apoderado_id) REFERENCES apoderados(id)
) ENGINE=InnoDB;

CREATE TABLE tipos_documento (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  obligatorio BOOLEAN NOT NULL DEFAULT TRUE,
  activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE matriculas (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  codigo VARCHAR(40) NOT NULL UNIQUE,
  estudiante_id BIGINT NOT NULL,
  apoderado_id BIGINT NULL,
  periodo_id BIGINT NOT NULL,
  seccion_id BIGINT NOT NULL,
  estado ENUM('PENDIENTE','ACTIVA','TRASLADADA',
    'RETIRADA','ANULADA') NOT NULL DEFAULT 'PENDIENTE',
  observacion VARCHAR(500) NULL,
  creado_por BIGINT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  periodo_activo BIGINT GENERATED ALWAYS AS
    (CASE WHEN estado = 'ACTIVA' THEN periodo_id
      ELSE NULL END) STORED,
  UNIQUE uq_matricula_activa (estudiante_id, periodo_activo),
  INDEX ix_seccion_estado (seccion_id, estado),
  CHECK (estado <> 'ACTIVA' OR apoderado_id IS NOT NULL),
  FOREIGN KEY (estudiante_id) REFERENCES estudiantes(id),
  FOREIGN KEY (estudiante_id, apoderado_id)
    REFERENCES estudiante_apoderado(estudiante_id, apoderado_id),
  FOREIGN KEY (periodo_id) REFERENCES periodos(id),
  FOREIGN KEY (seccion_id, periodo_id)
    REFERENCES secciones(id, periodo_id),
  FOREIGN KEY (creado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE documentos_matricula (
  matricula_id BIGINT NOT NULL,
  tipo_documento_id BIGINT NOT NULL,
  verificado BOOLEAN NOT NULL DEFAULT FALSE,
  verificado_por BIGINT NULL,
  verificado_en DATETIME NULL,
  PRIMARY KEY (matricula_id, tipo_documento_id),
  CHECK (verificado = FALSE OR
    (verificado_por IS NOT NULL AND verificado_en IS NOT NULL)),
  FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  FOREIGN KEY (tipo_documento_id) REFERENCES tipos_documento(id),
  FOREIGN KEY (verificado_por) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE movimientos_matricula (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  matricula_id BIGINT NOT NULL,
  tipo ENUM('CREACION','ACTIVACION','CAMBIO_SECCION',
    'RETIRO','ANULACION','TRASLADO_EXTERNO') NOT NULL,
  estado_anterior VARCHAR(20) NULL,
  estado_nuevo VARCHAR(20) NOT NULL,
  seccion_anterior_id BIGINT NULL,
  seccion_nueva_id BIGINT NULL,
  motivo VARCHAR(500) NOT NULL,
  usuario_id BIGINT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (matricula_id) REFERENCES matriculas(id),
  FOREIGN KEY (seccion_anterior_id) REFERENCES secciones(id),
  FOREIGN KEY (seccion_nueva_id) REFERENCES secciones(id),
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE auditoria (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  usuario_id BIGINT NOT NULL,
  accion VARCHAR(50) NOT NULL,
  entidad VARCHAR(50) NOT NULL,
  registro_id BIGINT NULL,
  detalle JSON NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  INDEX ix_auditoria_fecha (creado_en)
) ENGINE=InnoDB;

CREATE VIEW vw_vacantes AS
SELECT s.id AS seccion_id, s.periodo_id, s.grado_id,
       s.nombre, s.capacidad, s.activa,
       COUNT(m.id) AS ocupadas,
       s.capacidad - COUNT(m.id) AS disponibles
FROM secciones s LEFT JOIN matriculas m
  ON m.seccion_id = s.id AND m.estado = 'ACTIVA'
GROUP BY s.id, s.periodo_id, s.grado_id,
         s.nombre, s.capacidad, s.activa;
