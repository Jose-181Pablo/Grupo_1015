-- Terrenos tomados de inmobiliaria.html (sitio estático del Grupo 10.15).
-- "ON CONFLICT (clave) DO NOTHING" evita duplicarlos cada vez que arranca el servicio.

INSERT INTO terrenos (clave, nombre, ubicacion, precio, superficie_m2, medidas, uso_suelo, amenidades, imagen_url, ficha_tecnica_url, estatus, fecha_creacion, fecha_actualizacion) VALUES
('galerias_santa_anita', 'Galerías Santa Anita', 'Sobre Av. López Mateos, Santa Anita (Frente a Galerías)', NULL, 5615.42, NULL, 'Desarrollo Residencial Vertical', NULL, 'assets/images/GALERIAS_SANTA_ANITA/ingreso_terreno.jpg', 'assets/images/GALERIAS_SANTA_ANITA/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('lago_nogal', 'Lago Nogal', 'Fracc. Lago Nogal, Zona Sur de Guadalajara', 5490000, 448, '16 x 28 m (esquina)', 'Habitacional', 'Alberca, Gym, Vapor, Sauna, Tenis, Jogging, Cine, Terrazas', 'assets/images/LAGO_NOGAL/terreno_dia.jpg', 'assets/images/LAGO_NOGAL/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW()),
('bosques_santa_anita', 'Bosques de Santa Anita (Terreno)', 'Coto 9 Valles, Bosques de Santa Anita, Tlajomulco', NULL, 332.96, NULL, 'Habitacional', 'Áreas infantiles, capilla, terrazas para eventos', 'assets/images/BOSQUES_SANTA_ANITA/terreno_1.jpg', 'assets/images/BOSQUES_SANTA_ANITA/ficha_tecnica.pdf', 'DISPONIBLE', NOW(), NOW())
ON CONFLICT (clave) DO NOTHING;
