import { endpointCognito, regiaoDoDominio } from './cognitoProtocolo';

/**
 * Configuração do login com Cognito. Sem as variáveis do `mobile/.env`, o login fica desligado e o
 * app funciona como antes (backend em AUTH_MODO=seed).
 */
export const COGNITO_DOMAIN = (process.env.EXPO_PUBLIC_COGNITO_DOMAIN ?? '').replace(
  /\/+$/,
  ''
);

export const COGNITO_CLIENT_ID =
  process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? '';

export const authHabilitada = COGNITO_DOMAIN !== '' && COGNITO_CLIENT_ID !== '';

/** API do Cognito (login, renovação, saída), na região do domínio acima. */
export const ENDPOINT_COGNITO = endpointCognito(regiaoDoDominio(COGNITO_DOMAIN));
