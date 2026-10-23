export const environment = {
  production: false,
  apiUrl: '/api',
  oidc: {
    authority: 'https://accounts.google.com',
    client_id: 'YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com',
    redirect_uri: 'http://localhost:4200/callback',
    post_logout_redirect_uri: 'http://localhost:4200',
    response_type: 'code',
    scope: 'openid profile email',
    filterProtocolClaims: true,
    loadUserInfo: true,
  },
};
