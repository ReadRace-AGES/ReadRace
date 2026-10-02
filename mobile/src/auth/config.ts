/**
 * Configuração do login com Cognito. Sem as variáveis do `mobile/.env`, o login fica desligado e o
 * app funciona como antes (backend em AUTH_MODO=seed).
 */
const DOMAIN = (process.env.EXPO_PUBLIC_COGNITO_DOMAIN ?? '').replace(
  /\/+$/,
  ''
);

export const COGNITO_CLIENT_ID =
  process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? '';

export const authHabilitada = DOMAIN !== '' && COGNITO_CLIENT_ID !== '';

export const discovery = {
  authorizationEndpoint: `${DOMAIN}/oauth2/authorize`,
  tokenEndpoint: `${DOMAIN}/oauth2/token`,
  revocationEndpoint: `${DOMAIN}/oauth2/revoke`,
};

/** Mesma troca de código, mas a página do Cognito abre direto no cadastro. */
export const discoveryCadastro = {
  ...discovery,
  authorizationEndpoint: `${DOMAIN}/signup`,
};

/** Precisam bater com o `scheme` do app.config.js e com as callback URLs do app client. */
export const SCHEME = 'readrace';
export const REDIRECT_PATH = 'auth';
