import {
  configCognito,
  endpointCognito,
  regiaoDoDominio,
} from './cognitoProtocolo';

// O Expo só troca process.env.EXPO_PUBLIC_* pelo valor quando o acesso é direto, como aqui.
const config = configCognito(
  process.env.EXPO_PUBLIC_COGNITO_DOMAIN,
  process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID
);

/**
 * Configuração do login com Cognito. Sem as variáveis no `mobile/.env`, vale o user pool do projeto
 * e o login fica ligado, como a API em AUTH_MODO=cognito (padrão). Para desligar, deixe as duas
 * vazias no `.env` (API em AUTH_MODO=seed).
 */
export const COGNITO_DOMAIN = config.dominio;

export const COGNITO_CLIENT_ID = config.clientId;

export const authHabilitada = config.habilitada;

/** API do Cognito (login, renovação, saída), na região do domínio acima. */
export const ENDPOINT_COGNITO = endpointCognito(
  regiaoDoDominio(COGNITO_DOMAIN)
);
