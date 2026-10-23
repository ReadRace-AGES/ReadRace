import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';

import {
  chamarCognito,
  ESCOPOS_LOGIN_GOOGLE,
  erroDeCadastro,
  requisicaoCadastro,
  requisicaoConfirmacao,
  requisicaoReenvio,
  requisicaoLogin,
  requisicaoRenovacao,
  requisicaoRevogacao,
  resultadoDoLoginGoogle,
  sessaoDaResposta,
  sessaoDoTokenOAuth,
  urlPaginaCognito,
  type SessaoCognito,
} from './cognitoProtocolo';
import { COGNITO_CLIENT_ID, COGNITO_DOMAIN, ENDPOINT_COGNITO } from './config';

/** Lança ErroLogin com a mensagem pronta para a tela. */
export async function entrarComSenha(email: string, senha: string): Promise<SessaoCognito> {
  const corpo = await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoLogin(COGNITO_CLIENT_ID, email, senha)
  );
  return sessaoDaResposta(corpo, null, Date.now());
}

export async function renovarSessao(refreshToken: string): Promise<SessaoCognito> {
  const corpo = await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoRenovacao(COGNITO_CLIENT_ID, refreshToken)
  );
  return sessaoDaResposta(corpo, refreshToken, Date.now());
}

export async function revogarSessao(refreshToken: string): Promise<void> {
  await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoRevogacao(COGNITO_CLIENT_ID, refreshToken)
  );
}

/** Lança ErroCadastro com a mensagem pronta para a tela. O Cognito manda o código por e-mail. */
export async function criarConta(nome: string, email: string, senha: string): Promise<void> {
  await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoCadastro(COGNITO_CLIENT_ID, nome, email, senha),
    erroDeCadastro('cadastro')
  );
}

export async function confirmarConta(email: string, codigo: string): Promise<void> {
  await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoConfirmacao(COGNITO_CLIENT_ID, email, codigo),
    erroDeCadastro('confirmacao')
  );
}

export async function reenviarCodigo(email: string): Promise<void> {
  await chamarCognito(
    fetch,
    ENDPOINT_COGNITO,
    requisicaoReenvio(COGNITO_CLIENT_ID, email),
    erroDeCadastro('reenvio')
  );
}

/**
 * Recuperar senha continua na página do Cognito. Navegador simples, sem esperar código: depois a
 * pessoa entra pela tela do app.
 */
export async function abrirPaginaCognito(pagina: 'forgotPassword'): Promise<void> {
  await WebBrowser.openBrowserAsync(urlPaginaCognito(COGNITO_DOMAIN, COGNITO_CLIENT_ID, pagina));
}

const paginasOAuth = {
  authorizationEndpoint: `${COGNITO_DOMAIN}/oauth2/authorize`,
  tokenEndpoint: `${COGNITO_DOMAIN}/oauth2/token`,
};

/**
 * Abre a página do Cognito indo direto para a escolha de conta do Google (identity_provider) e
 * troca o código pelo token, com PKCE. `null` = a pessoa desistiu; qualquer outra falha lança erro.
 * O retorno é readrace://auth num development build; no Expo Go o endereço é outro e o Cognito
 * recusa, porque só readrace://auth está cadastrado no app client.
 */
export async function entrarComGoogle(): Promise<SessaoCognito | null> {
  const redirectUri = AuthSession.makeRedirectUri({ scheme: 'readrace', path: 'auth' });
  const pedido = new AuthSession.AuthRequest({
    clientId: COGNITO_CLIENT_ID,
    redirectUri,
    scopes: ESCOPOS_LOGIN_GOOGLE,
    usePKCE: true,
    extraParams: { identity_provider: 'Google' },
  });

  const resposta = await pedido.promptAsync(paginasOAuth);
  const resultado = resultadoDoLoginGoogle(
    resposta.type,
    'params' in resposta ? resposta.params : undefined
  );
  if (resultado.tipo === 'desistiu') {
    return null;
  }
  if (resultado.tipo === 'erro' || !pedido.codeVerifier) {
    throw new Error('O Cognito não devolveu o código do login com Google.');
  }

  const token = await AuthSession.exchangeCodeAsync(
    {
      clientId: COGNITO_CLIENT_ID,
      code: resultado.codigo,
      redirectUri,
      extraParams: { code_verifier: pedido.codeVerifier },
    },
    paginasOAuth
  );
  return sessaoDoTokenOAuth(token, Date.now());
}
