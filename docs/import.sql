USE gestion_congresos;
-- Insertar roles
INSERT INTO roles (role_name) VALUES
("UNKNOWN"),
('ADMINISTRATOR'),
('EVALUATOR'),
('EXPOSITOR'),
('LISTENER');


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


INSERT INTO papers (paper_id, title, code, version, summary, keywords, presentation_date, status, users_reviewer_id, congresses_id)
VALUES (
    100, 
    'Impacto de la Inteligencia Artificial en la Educación Universitaria', 
    'PAPER-IA-001', 
    '2.0', 
    'Este trabajo analiza cómo las herramientas de IA generativa están cambiando el paradigma educativo...', 
    'IA, Educación, Universidad, Tecnología', 
    '2024-11-20 10:00:00', 
    'UNDER_EVALUATION', 
    2, 
    1
);


INSERT INTO evaluations (feedback, new_deadline, new_status, paper_id, evaluation_date, evaluated_version)
VALUES (
    'El marco teórico es interesante, pero falta profundizar en los casos de estudio. Por favor, ampliar la sección 3.', 
    '2024-10-15 23:59:59', 
    "NEEDS_REVISION",
    100, 
    '2024-10-01 15:30:00', 
    '1.0' 
);

INSERT INTO evaluations (feedback, new_deadline, new_status, paper_id, evaluation_date, evaluated_version)
VALUES (
    'Las correcciones fueron aplicadas correctamente. Excelente trabajo.', 
    NULL, 
     "ACCEPTED", 
     100, 
    '2024-10-18 10:15:00', 
    '2.0' 
);