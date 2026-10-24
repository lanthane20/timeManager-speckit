#!/bin/sh
# Remplace les marqueurs ${GOOGLE_CLIENT_ID} et ${APP_BASE_URL} de environment.prod.ts dans le
# build, comme `npm run inject-env` (frontend/scripts/inject-env.mjs) hors conteneur.
set -eu

HTML_DIR=/usr/share/nginx/html

# Déjà injecté (redémarrage d'un conteneur existant) : rien à faire.
if ! grep -qlF '${GOOGLE_CLIENT_ID}' "$HTML_DIR"/*.js 2>/dev/null; then
  echo "inject-env : configuration déjà injectée"
  exit 0
fi

for name in GOOGLE_CLIENT_ID APP_BASE_URL; do
  value=$(printenv "$name" || true)
  # Valeurs insérées dans des chaînes JavaScript : aucun caractère ne doit pouvoir en sortir.
  if ! printf '%s' "$value" | grep -qE '^[A-Za-z0-9_.:/-]+$'; then
    echo "inject-env : variable $name absente ou invalide" >&2
    exit 1
  fi
done

APP_BASE_URL=${APP_BASE_URL%/}
for file in "$HTML_DIR"/*.js; do
  sed -i -e "s|\${GOOGLE_CLIENT_ID}|$GOOGLE_CLIENT_ID|g" -e "s|\${APP_BASE_URL}|$APP_BASE_URL|g" "$file"
done
echo "inject-env : configuration de production injectée"
