// Les marqueurs ${…} sont remplacés dans le build par `npm run inject-env` (voir quickstart.md).
export const environment = {
  production: true,
  apiUrl: '/api',
  oidc: {
    authority: 'https://accounts.google.com',
    client_id: '${GOOGLE_CLIENT_ID}',
    redirect_uri: '${APP_BASE_URL}/callback',
    post_logout_redirect_uri: '${APP_BASE_URL}',
    response_type: 'code',
    scope: 'openid profile email',
    filterProtocolClaims: true,
    loadUserInfo: true,
  },
};
