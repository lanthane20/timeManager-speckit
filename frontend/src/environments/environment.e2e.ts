// Tests end-to-end : le Keycloak de test (frontend/e2e/keycloak) remplace Google.
export const environment = {
  production: false,
  apiUrl: '/api',
  oidc: {
    authority: 'http://localhost:8180/realms/timeplanning',
    client_id: 'timeplanning-frontend',
    redirect_uri: 'http://localhost:4200/callback',
    post_logout_redirect_uri: 'http://localhost:4200',
    response_type: 'code',
    scope: 'openid profile email',
    filterProtocolClaims: true,
    loadUserInfo: true,
  },
};
