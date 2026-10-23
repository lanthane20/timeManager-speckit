-- Jeu de données des tests end-to-end : comptes de démonstration de fixtures/auth.ts.
-- À charger après les migrations Liquibase, sur une base dédiée aux tests.
-- google_sub = identifiant (sub) des utilisateurs du Keycloak de test (keycloak/timeplanning-realm.json).

INSERT INTO users (id, google_sub, email, first_name, last_name, active, recurrence_enabled, created_at)
VALUES
  ('e2e00000-0000-0000-0000-000000000001', '6f2d8a1e-3b4c-4d5e-8f60-000000000001', 'lea.martin@timemanager.fr', 'Léa', 'Martin', true, false, NOW()),
  ('e2e00000-0000-0000-0000-000000000002', '6f2d8a1e-3b4c-4d5e-8f60-000000000002', 'manon.dubois@timemanager.fr', 'Manon', 'Dubois', true, false, NOW()),
  ('e2e00000-0000-0000-0000-000000000003', '6f2d8a1e-3b4c-4d5e-8f60-000000000003', 'rachel.hubert@timemanager.fr', 'Rachel', 'Hubert', true, false, NOW());

INSERT INTO user_roles (user_id, role)
VALUES
  ('e2e00000-0000-0000-0000-000000000001', 'SALARIE'),
  ('e2e00000-0000-0000-0000-000000000002', 'SALARIE'),
  ('e2e00000-0000-0000-0000-000000000002', 'MANAGER'),
  ('e2e00000-0000-0000-0000-000000000003', 'SALARIE'),
  ('e2e00000-0000-0000-0000-000000000003', 'RH');

INSERT INTO teams (id, name, validation_mode, n1_manager_id, active)
VALUES ('e2e00000-0000-0000-0000-0000000000a1', 'Équipe E2E', 'N1_ONLY', 'e2e00000-0000-0000-0000-000000000002', true);

INSERT INTO team_members (team_id, user_id)
VALUES ('e2e00000-0000-0000-0000-0000000000a1', 'e2e00000-0000-0000-0000-000000000001');
