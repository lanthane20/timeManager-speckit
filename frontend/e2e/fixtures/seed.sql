-- Jeu de données des tests end-to-end : comptes de démonstration de fixtures/auth.ts.
-- À charger après les migrations Liquibase, sur une base dédiée aux tests.

INSERT INTO users (id, google_sub, email, first_name, last_name, active, recurrence_enabled, created_at)
VALUES
  ('e2e00000-0000-0000-0000-000000000001', 'e2e-salarie', 'lea.martin@timemanager.fr', 'Léa', 'Martin', true, false, NOW()),
  ('e2e00000-0000-0000-0000-000000000002', 'e2e-manager', 'manon.dubois@timemanager.fr', 'Manon', 'Dubois', true, false, NOW()),
  ('e2e00000-0000-0000-0000-000000000003', 'e2e-rh', 'rachel.hubert@timemanager.fr', 'Rachel', 'Hubert', true, false, NOW());

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
