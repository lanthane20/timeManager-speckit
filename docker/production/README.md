# Déploiement en production — serveur privé

Déploiement continu sans port entrant : un **runner GitHub Actions auto-hébergé** installé sur le
serveur exécute le job `deploy` de [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml).

```
push sur master → CI verte → images publiées sur ghcr.io → runner du serveur :
  sauvegarde MariaDB → docker compose pull → docker compose up -d --wait → contrôle de santé
```

| Service | Image | Exposition |
|---------|-------|------------|
| `traefik` | `traefik:v3.6` | Ports 80 (redirigé) et 443 |
| `frontend` | `ghcr.io/lanthane20/timemanager-speckit-frontend` | Via Traefik : `/` |
| `backend` | `ghcr.io/lanthane20/timemanager-speckit-backend` | Via Traefik : `/api` uniquement (`/actuator` reste interne) |
| `db` | `mariadb:11.4` | Réseau interne uniquement |

## 1. Préparer le serveur (une fois)

1. Installer Docker Engine et le plugin Compose (Docker 29 ou plus récent).
2. Créer un utilisateur dédié au runner, membre du groupe `docker`, et le répertoire de déploiement :

   ```bash
   sudo useradd -m -G docker github-runner
   sudo mkdir -p /opt/timeplanning/certs && sudo chown -R github-runner: /opt/timeplanning
   ```

3. Copier [`.env.example`](./.env.example) en `/opt/timeplanning/.env` et renseigner les vraies
   valeurs (`chmod 600`). Ce fichier ne doit jamais être versionné.
4. Certificat HTTPS, au choix :
   - **Autorité interne** (serveur non joignable depuis Internet) : déposer
     `/opt/timeplanning/certs/timeplanning.crt` et `timeplanning.key`. C'est la configuration par
     défaut.
   - **Let's Encrypt** (port 80 joignable depuis Internet) : suivre le commentaire de
     `docker-compose.yml` et renseigner `ACME_EMAIL`.

## 2. Installer le runner auto-hébergé

1. GitHub : *Settings → Actions → Runners → New self-hosted runner* (Linux) et dérouler les
   commandes affichées, **en tant que `github-runner`**.
2. À l'étape `config.sh`, ajouter le libellé attendu par le workflow : `--labels timeplanning`.
3. L'installer comme service pour qu'il redémarre avec le serveur :

   ```bash
   sudo ./svc.sh install github-runner && sudo ./svc.sh start
   ```

> **Sécurité** : un runner auto-hébergé exécute le code des workflows du dépôt. Ne l'associer qu'à ce
> dépôt privé, jamais à un dépôt public.

## 3. Configurer GitHub

- *Settings → Environments → New environment* : `production`. Ajouter des *Required reviewers*
  pour valider manuellement chaque mise en production (recommandé).
- Les images sont publiées avec le `GITHUB_TOKEN` du workflow : aucun secret à créer.

## 4. Déployer

- **Automatique** : chaque CI verte sur `master` publie les images et déploie.
- **Manuel** : *Actions → Deploy → Run workflow* ; tag vide = construire et déployer `master`.
- **Retour arrière** : *Run workflow* avec le SHA d'un commit déjà déployé comme tag. Les images
  sont conservées sur ghcr.io par SHA.

## Exploitation

- Sauvegardes : `/opt/timeplanning/backups/`, une avant chaque déploiement, 14 conservées.
  Restauration :

  ```bash
  cd /opt/timeplanning
  gunzip -c backups/timemanager-AAAAMMJJ-HHMMSS.sql.gz \
    | docker compose exec -T db sh -c 'mariadb -uroot -p"$MARIADB_ROOT_PASSWORD" timemanager'
  ```

- Journaux : `docker compose logs -f backend` (JSON structuré, profil `prod`).
- Le contrôle de santé du backend inclut le serveur mail : un SMTP injoignable fait échouer le
  déploiement. Pour l'ignorer, ajouter `MANAGEMENT_HEALTH_MAIL_ENABLED=false` au `.env`.
- Le frontend refuse de démarrer si `GOOGLE_CLIENT_ID` ou `APP_BASE_URL` manque ou contient un
  caractère hors `[A-Za-z0-9_.:/-]`.
