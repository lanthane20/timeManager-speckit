# Image du frontend : build Angular de production, servi par Nginx.
# Contexte de build : la racine du dépôt (docker build -f docker/frontend/Dockerfile .).
FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund
COPY frontend/ .
RUN npm run build -- --configuration=production

FROM nginx:1.28-alpine AS runtime
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
# Exécuté par l'entrypoint Nginx à chaque démarrage : injecte GOOGLE_CLIENT_ID et APP_BASE_URL.
COPY --chmod=755 docker/frontend/40-inject-env.sh /docker-entrypoint.d/40-inject-env.sh
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ > /dev/null || exit 1
