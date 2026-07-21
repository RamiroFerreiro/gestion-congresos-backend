USE gestion_congresos;
-- Insertar roles
INSERT INTO roles (name) VALUES
('DESCONOCIDO'),
('ADMINISTRADOR'),
('EVALUADOR'),
('EXPOSITOR'),
('OYENTE');


-- Insertar usuarios

-- Usuario administrador
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    dni,
    institution,
    country,
    enabled,
    roles_id
) VALUES (
    'Carlos',
    'Gomez',
    'admin@congreso.com',
    '123456',
    30111222,
    'Universidad Nacional de Lanús',
    'Argentina',
    true,
    2
);


-- Usuario evaluador
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    dni,
    institution,
    country,
    enabled,
    roles_id
) VALUES (
    'Maria',
    'Lopez',
    'evaluador@congreso.com',
    '123456',
    32222333,
    'Universidad Nacional de Lanús',
    'Argentina',
    true,
    3
);


-- Usuario expositor
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    dni,
    institution,
    country,
    enabled,
    roles_id
) VALUES (
    'Juan',
    'Perez',
    'expositor@congreso.com',
    '123456',
    34444555,
    'Universidad Nacional de La Plata',
    'Argentina',
    true,
    4
);


-- Usuario oyente
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    dni,
    institution,
    country,
    enabled,
    roles_id
) VALUES (
    'Ana',
    'Martinez',
    'oyente@congreso.com',
    '123456',
    36666777,
    'Universidad de Buenos Aires',
    'Argentina',
    true,
    5
);


-- Usuario desconocido
INSERT INTO users (
    first_name,
    last_name,
    email,
    password,
    dni,
    institution,
    country,
    enabled,
    roles_id
) VALUES (
    'Pedro',
    'Rodriguez',
    'desconocido@congreso.com',
    '123456',
    38888999,
    'Universidad Nacional de Lanús',
    'Argentina',
    true,
    1
);