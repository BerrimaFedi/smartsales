-- =============================================================================
-- SmartSales — Données initiales Grand Tunis
-- Exécuté par le service "seeder" APRÈS que Hibernate ait créé les tables
-- et que le DataInitializer ait créé l'utilisateur admin (admin/admin).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Zones géographiques du Grand Tunis
-- -----------------------------------------------------------------------------
INSERT INTO zones (nom, description) VALUES
  ('Tunis Centre',  'Medina, Lafayette, Le Bardo et quartiers centraux'),
  ('Ariana',        'Ariana Ville, Raoued, La Soukra, Sidi Thabet'),
  ('Ben Arous',     'Ben Arous, Hammam Lif, Rades, Megrine, Mohamedia'),
  ('Manouba',       'Manouba Ville, Den Den, Douar Hicher, Oued Ellil'),
  ('La Marsa',      'La Marsa, Gammarth, Sidi Bou Said, Carthage')
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2. Compétences commerciales
-- -----------------------------------------------------------------------------
INSERT INTO competences (nom) VALUES
  ('Prospection'),
  ('Négociation'),
  ('Fidélisation'),
  ('Gestion Grands Comptes'),
  ('Service Client'),
  ('Secteur Technologie'),
  ('Secteur Distribution')
ON CONFLICT (nom) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3. Utilisateurs commerciaux
--    On réutilise le hash BCrypt de l'admin créé par Hibernate/DataInitializer
--    (mot de passe "admin" pour tous — à changer en production).
-- -----------------------------------------------------------------------------
DO $$
DECLARE
  h TEXT;
BEGIN
  SELECT password INTO h FROM users WHERE username = 'admin';
  IF h IS NULL THEN
    RAISE EXCEPTION 'Utilisateur admin introuvable — DataInitializer non exécuté ?';
  END IF;

  INSERT INTO users (username, email, password, role, enabled) VALUES
    ('sami',   'sami@smartsales.local',   h, 'COMMERCIAL', true),
    ('leila',  'leila@smartsales.local',  h, 'COMMERCIAL', true),
    ('karim',  'karim@smartsales.local',  h, 'COMMERCIAL', true),
    ('nadia',  'nadia@smartsales.local',  h, 'COMMERCIAL', true),
    ('ahmed',  'ahmed@smartsales.local',  h, 'COMMERCIAL', true)
  ON CONFLICT (username) DO NOTHING;
END $$;

-- -----------------------------------------------------------------------------
-- 4. Commerciaux (liés aux users et zones)
-- -----------------------------------------------------------------------------
INSERT INTO commerciaux (user_id, nom, prenom, telephone, zone_id)
SELECT u.id, 'Ben Ali',     'Sami',   '+216 22 100 001',
       (SELECT id FROM zones WHERE nom = 'Tunis Centre')
FROM users u WHERE u.username = 'sami'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO commerciaux (user_id, nom, prenom, telephone, zone_id)
SELECT u.id, 'Mansour',    'Leila',  '+216 22 100 002',
       (SELECT id FROM zones WHERE nom = 'Ariana')
FROM users u WHERE u.username = 'leila'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO commerciaux (user_id, nom, prenom, telephone, zone_id)
SELECT u.id, 'Oulad Haj',  'Karim',  '+216 22 100 003',
       (SELECT id FROM zones WHERE nom = 'Ben Arous')
FROM users u WHERE u.username = 'karim'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO commerciaux (user_id, nom, prenom, telephone, zone_id)
SELECT u.id, 'Trabelsi',   'Nadia',  '+216 22 100 004',
       (SELECT id FROM zones WHERE nom = 'Manouba')
FROM users u WHERE u.username = 'nadia'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO commerciaux (user_id, nom, prenom, telephone, zone_id)
SELECT u.id, 'Gafsi',      'Ahmed',  '+216 22 100 005',
       (SELECT id FROM zones WHERE nom = 'La Marsa')
FROM users u WHERE u.username = 'ahmed'
ON CONFLICT (user_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5. Compétences par commercial
-- -----------------------------------------------------------------------------
-- Sami : Prospection, Négociation, Secteur Technologie
INSERT INTO commercial_competences (commercial_id, competence_id)
SELECT c.id, cp.id
FROM commerciaux c, competences cp
WHERE c.user_id = (SELECT id FROM users WHERE username = 'sami')
  AND cp.nom IN ('Prospection', 'Négociation', 'Secteur Technologie')
ON CONFLICT DO NOTHING;

-- Leila : Fidélisation, Gestion Grands Comptes, Service Client
INSERT INTO commercial_competences (commercial_id, competence_id)
SELECT c.id, cp.id
FROM commerciaux c, competences cp
WHERE c.user_id = (SELECT id FROM users WHERE username = 'leila')
  AND cp.nom IN ('Fidélisation', 'Gestion Grands Comptes', 'Service Client')
ON CONFLICT DO NOTHING;

-- Karim : Prospection, Secteur Distribution, Négociation
INSERT INTO commercial_competences (commercial_id, competence_id)
SELECT c.id, cp.id
FROM commerciaux c, competences cp
WHERE c.user_id = (SELECT id FROM users WHERE username = 'karim')
  AND cp.nom IN ('Prospection', 'Secteur Distribution', 'Négociation')
ON CONFLICT DO NOTHING;

-- Nadia : Service Client, Fidélisation, Gestion Grands Comptes
INSERT INTO commercial_competences (commercial_id, competence_id)
SELECT c.id, cp.id
FROM commerciaux c, competences cp
WHERE c.user_id = (SELECT id FROM users WHERE username = 'nadia')
  AND cp.nom IN ('Service Client', 'Fidélisation', 'Gestion Grands Comptes')
ON CONFLICT DO NOTHING;

-- Ahmed : Prospection, Négociation, Secteur Technologie, Secteur Distribution
INSERT INTO commercial_competences (commercial_id, competence_id)
SELECT c.id, cp.id
FROM commerciaux c, competences cp
WHERE c.user_id = (SELECT id FROM users WHERE username = 'ahmed')
  AND cp.nom IN ('Prospection', 'Négociation', 'Secteur Technologie', 'Secteur Distribution')
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 6. Clients (Grand Tunis, avec coordonnées GPS réelles)
-- -----------------------------------------------------------------------------
INSERT INTO clients (nom, adresse, telephone, latitude, longitude, zone_id) VALUES
  -- Tunis Centre
  ('Poulina Group Holding',      'Rue du Lac Windermere, Les Berges du Lac, Tunis',    '+216 71 960 000', 36.8304, 10.2297,
   (SELECT id FROM zones WHERE nom = 'Tunis Centre')),
  ('BIAT Siège Social',          'Av. Habib Bourguiba, Tunis',                         '+216 71 131 000', 36.7987, 10.1783,
   (SELECT id FROM zones WHERE nom = 'Tunis Centre')),
  ('Orange Tunisie',             'Av. Jugurtha, Mutuelle Ville, Tunis',                '+216 71 770 000', 36.8297, 10.1678,
   (SELECT id FROM zones WHERE nom = 'Tunis Centre')),
  ('Tunisie Telecom Siège',      'Av. Hédi Nouira, Ariana Soghra, Tunis',             '+216 71 802 400', 36.8220, 10.1850,
   (SELECT id FROM zones WHERE nom = 'Tunis Centre')),
  ('STB Siège',                  'Rue Hédi Karray, Tunis',                             '+216 71 340 477', 36.8157, 10.1775,
   (SELECT id FROM zones WHERE nom = 'Tunis Centre')),

  -- Ariana
  ('SOTUVER Ariana',             'Zone Industrielle, La Soukra, Ariana',               '+216 71 761 200', 36.8895, 10.1979,
   (SELECT id FROM zones WHERE nom = 'Ariana')),
  ('Amen Bank Ariana',           'Av. de la Liberté, Ariana',                          '+216 71 163 900', 36.8621, 10.1596,
   (SELECT id FROM zones WHERE nom = 'Ariana')),
  ('Pharmacie El Amal',          'Av. Tahar Ben Ammar, Raoued, Ariana',                '+216 71 763 050', 36.8982, 10.1526,
   (SELECT id FROM zones WHERE nom = 'Ariana')),
  ('Librairie El Majd',          'Cité El Ghazala, Ariana',                            '+216 71 857 320', 36.8710, 10.1700,
   (SELECT id FROM zones WHERE nom = 'Ariana')),

  -- Ben Arous
  ('Groupe Chimique Tunisien',   'Route de Gabès, Radès, Ben Arous',                  '+216 71 430 200', 36.7748, 10.2721,
   (SELECT id FROM zones WHERE nom = 'Ben Arous')),
  ('SOTRAPIL',                   'Zone Industrielle, Mégrine, Ben Arous',              '+216 71 398 000', 36.7460, 10.2310,
   (SELECT id FROM zones WHERE nom = 'Ben Arous')),
  ('COFAT',                      'Av. Farhat Hached, Ben Arous',                       '+216 71 388 900', 36.7531, 10.2278,
   (SELECT id FROM zones WHERE nom = 'Ben Arous')),
  ('Centrale Laitière Tunisie',  'Zone Industrielle Mohamedia, Ben Arous',             '+216 71 430 800', 36.7370, 10.2060,
   (SELECT id FROM zones WHERE nom = 'Ben Arous')),

  -- Manouba
  ('Imprimerie Finzi',           'Route de Bizerte, Douar Hicher, Manouba',            '+216 71 508 300', 36.8200, 10.1120,
   (SELECT id FROM zones WHERE nom = 'Manouba')),
  ('Centre Commercial Manouba',  'Av. Habib Bourguiba, Manouba',                       '+216 71 600 500', 36.8097, 10.0964,
   (SELECT id FROM zones WHERE nom = 'Manouba')),
  ('Atelier Méca Den Den',       'Cité Den Den, Manouba',                              '+216 71 520 110', 36.8315, 10.1280,
   (SELECT id FROM zones WHERE nom = 'Manouba')),

  -- La Marsa
  ('Carthago Consulting',        'Av. Habib Bourguiba, Carthage, Tunis',               '+216 71 732 000', 36.8525, 10.3233,
   (SELECT id FROM zones WHERE nom = 'La Marsa')),
  ('Hôtel Résidence Sidi Bou',   'Rue Sidi Bou Said, Sidi Bou Said',                  '+216 71 740 900', 36.8703, 10.3417,
   (SELECT id FROM zones WHERE nom = 'La Marsa')),
  ('Agence TOPNET La Marsa',     'Av. Taieb Mehiri, La Marsa',                        '+216 71 749 300', 36.8778, 10.3236,
   (SELECT id FROM zones WHERE nom = 'La Marsa')),
  ('Sté de Distribution Gammarth','Route Touristique, Gammarth',                       '+216 71 911 500', 36.9069, 10.2978,
   (SELECT id FROM zones WHERE nom = 'La Marsa'))
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 7. Visites commerciales
-- -----------------------------------------------------------------------------
-- Sami (Tunis Centre) — PROSPECTION + NEGOCIATION
INSERT INTO visites (commercial_id, client_id, date_visite, type, statut, compte_rendu, montant, ordre_tournee)
SELECT
  (SELECT c.id FROM commerciaux c JOIN users u ON c.user_id = u.id WHERE u.username = 'sami'),
  cl.id,
  v.dv::timestamp, v.t, v.s, v.cr, v.m::numeric, v.o::int
FROM (VALUES
  ('Poulina Group Holding',   '2026-05-10 09:00', 'PROSPECTION',  'TERMINEE',  'Premier contact positif, présentation catalogue.', '0',       '1'),
  ('BIAT Siège Social',       '2026-05-10 11:30', 'NEGOCIATION',  'TERMINEE',  'Proposition commerciale envoyée suite à la réunion.',  '45000.00', '2'),
  ('Orange Tunisie',          '2026-05-12 09:30', 'PROSPECTION',  'TERMINEE',  'Rendez-vous fixé avec le DSI pour la semaine suivante.', '0',      '1'),
  ('STB Siège',               '2026-05-14 14:00', 'RELANCE',      'TERMINEE',  'Relance offre n°2026-04, intérêt confirmé.',         '12000.00', '3'),
  ('Tunisie Telecom Siège',   '2026-06-03 10:00', 'NEGOCIATION',  'PLANIFIEE', NULL,                                                  '0',       '2'),
  ('BIAT Siège Social',       '2026-06-05 09:00', 'RELANCE',      'PLANIFIEE', NULL,                                                  '0',       '1')
) AS v(cn, dv, t, s, cr, m, o)
JOIN clients cl ON cl.nom = v.cn
ON CONFLICT DO NOTHING;

-- Leila (Ariana) — fidélisation clients existants
INSERT INTO visites (commercial_id, client_id, date_visite, type, statut, compte_rendu, montant, ordre_tournee)
SELECT
  (SELECT c.id FROM commerciaux c JOIN users u ON c.user_id = u.id WHERE u.username = 'leila'),
  cl.id,
  v.dv::timestamp, v.t, v.s, v.cr, v.m::numeric, v.o::int
FROM (VALUES
  ('SOTUVER Ariana',    '2026-05-06 09:00', 'NEGOCIATION',  'TERMINEE',  'Accord cadre annuel signé 60 000 DT.', '60000.00', '1'),
  ('Amen Bank Ariana',  '2026-05-06 11:00', 'RELANCE',      'TERMINEE',  'Suivi contrat maintenance, renouvellement confirmé.', '8500.00', '2'),
  ('Pharmacie El Amal', '2026-05-08 14:00', 'PROSPECTION',  'TERMINEE',  'Présentation gamme produits, intérêt pour l''offre PME.', '0', '3'),
  ('Librairie El Majd', '2026-05-15 10:00', 'PROSPECTION',  'TERMINEE',  'Pas de suite immédiate, à recontacter en juillet.', '0', '1'),
  ('SOTUVER Ariana',    '2026-06-10 09:30', 'RELANCE',      'PLANIFIEE', NULL, '0', '1'),
  ('Amen Bank Ariana',  '2026-06-12 11:00', 'NEGOCIATION',  'PLANIFIEE', NULL, '0', '2')
) AS v(cn, dv, t, s, cr, m, o)
JOIN clients cl ON cl.nom = v.cn
ON CONFLICT DO NOTHING;

-- Karim (Ben Arous)
INSERT INTO visites (commercial_id, client_id, date_visite, type, statut, compte_rendu, montant, ordre_tournee)
SELECT
  (SELECT c.id FROM commerciaux c JOIN users u ON c.user_id = u.id WHERE u.username = 'karim'),
  cl.id,
  v.dv::timestamp, v.t, v.s, v.cr, v.m::numeric, v.o::int
FROM (VALUES
  ('Groupe Chimique Tunisien', '2026-05-05 08:30', 'NEGOCIATION', 'TERMINEE', 'Contrat fournitures industrielles 120 000 DT/an.', '120000.00', '1'),
  ('SOTRAPIL',                 '2026-05-05 11:00', 'PROSPECTION', 'TERMINEE', 'Démonstration produit. Devis demandé.', '0', '2'),
  ('COFAT',                    '2026-05-07 09:00', 'RELANCE',     'TERMINEE', 'Devis accepté, bon de commande en cours.', '34000.00', '3'),
  ('Centrale Laitière Tunisie','2026-05-20 14:30', 'PROSPECTION', 'TERMINEE', 'Potentiel 80 000 DT. Suite réunion direction.', '0', '1'),
  ('Groupe Chimique Tunisien', '2026-06-09 09:00', 'RELANCE',     'PLANIFIEE', NULL, '0', '1'),
  ('COFAT',                    '2026-06-11 10:00', 'NEGOCIATION', 'PLANIFIEE', NULL, '0', '2')
) AS v(cn, dv, t, s, cr, m, o)
JOIN clients cl ON cl.nom = v.cn
ON CONFLICT DO NOTHING;

-- Nadia (Manouba)
INSERT INTO visites (commercial_id, client_id, date_visite, type, statut, compte_rendu, montant, ordre_tournee)
SELECT
  (SELECT c.id FROM commerciaux c JOIN users u ON c.user_id = u.id WHERE u.username = 'nadia'),
  cl.id,
  v.dv::timestamp, v.t, v.s, v.cr, v.m::numeric, v.o::int
FROM (VALUES
  ('Imprimerie Finzi',          '2026-05-11 09:00', 'NEGOCIATION', 'TERMINEE', 'Renouvellement contrat annuel papeterie.', '18000.00', '1'),
  ('Centre Commercial Manouba', '2026-05-11 11:30', 'PROSPECTION', 'TERMINEE', 'Réunion merchandising, offre en attente.', '0', '2'),
  ('Atelier Méca Den Den',      '2026-05-13 14:00', 'RELANCE',     'TERMINEE', 'Commande confirmée 9 500 DT.', '9500.00', '3'),
  ('Centre Commercial Manouba', '2026-06-08 10:00', 'NEGOCIATION', 'PLANIFIEE', NULL, '0', '1'),
  ('Imprimerie Finzi',          '2026-06-15 09:00', 'RELANCE',     'PLANIFIEE', NULL, '0', '2')
) AS v(cn, dv, t, s, cr, m, o)
JOIN clients cl ON cl.nom = v.cn
ON CONFLICT DO NOTHING;

-- Ahmed (La Marsa)
INSERT INTO visites (commercial_id, client_id, date_visite, type, statut, compte_rendu, montant, ordre_tournee)
SELECT
  (SELECT c.id FROM commerciaux c JOIN users u ON c.user_id = u.id WHERE u.username = 'ahmed'),
  cl.id,
  v.dv::timestamp, v.t, v.s, v.cr, v.m::numeric, v.o::int
FROM (VALUES
  ('Carthago Consulting',            '2026-05-08 09:00', 'NEGOCIATION', 'TERMINEE', 'Contrat conseil SI 55 000 DT signé.', '55000.00', '1'),
  ('Hôtel Résidence Sidi Bou',       '2026-05-08 11:30', 'PROSPECTION', 'TERMINEE', 'Présentation offre événementiel, budget 2026 disponible.', '0', '2'),
  ('Agence TOPNET La Marsa',         '2026-05-09 14:00', 'RELANCE',     'TERMINEE', 'Upgrade fibra accepté, 6 000 DT/an.', '6000.00', '3'),
  ('Sté de Distribution Gammarth',   '2026-05-21 10:00', 'PROSPECTION', 'TERMINEE', 'Nouvelle opportunité logistique détectée.', '0', '4'),
  ('Carthago Consulting',            '2026-06-06 09:00', 'RELANCE',     'PLANIFIEE', NULL, '0', '1'),
  ('Hôtel Résidence Sidi Bou',       '2026-06-13 11:00', 'NEGOCIATION', 'PLANIFIEE', NULL, '0', '2')
) AS v(cn, dv, t, s, cr, m, o)
JOIN clients cl ON cl.nom = v.cn
ON CONFLICT DO NOTHING;

-- -----------------------------------------------------------------------------
-- 8. Performances mensuelles (périodes 2026-03 à 2026-05)
-- -----------------------------------------------------------------------------
INSERT INTO performances (commercial_id, periode, chiffre_affaires, nombre_visites, taux_conversion)
SELECT c.id, p.periode, p.ca, p.nv, p.tc
FROM (VALUES
  -- Sami
  ('sami', '2026-03', 35000.00, 8,  62.5),
  ('sami', '2026-04', 48000.00, 10, 70.0),
  ('sami', '2026-05', 57000.00, 12, 75.0),
  -- Leila
  ('leila', '2026-03', 52000.00, 9,  77.8),
  ('leila', '2026-04', 68500.00, 11, 81.8),
  ('leila', '2026-05', 68500.00, 10, 80.0),
  -- Karim
  ('karim', '2026-03', 95000.00, 7,  71.4),
  ('karim', '2026-04', 110000.00, 9, 77.8),
  ('karim', '2026-05', 154000.00, 11, 81.8),
  -- Nadia
  ('nadia', '2026-03', 22000.00, 6,  50.0),
  ('nadia', '2026-04', 27500.00, 8,  62.5),
  ('nadia', '2026-05', 27500.00, 9,  66.7),
  -- Ahmed
  ('ahmed', '2026-03', 44000.00, 8,  62.5),
  ('ahmed', '2026-04', 61000.00, 10, 70.0),
  ('ahmed', '2026-05', 61000.00, 11, 72.7)
) AS p(username, periode, ca, nv, tc)
JOIN commerciaux c ON c.user_id = (SELECT id FROM users WHERE username = p.username)
ON CONFLICT DO NOTHING;
