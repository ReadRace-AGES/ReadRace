import * as WebBrowser from 'expo-web-browser';

import {
  chamarCognito,
  requisicaoLogin,
  requisicaoRenovacao,
  requisicaoRevogacao,
  sessaoDaResposta,
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

/**
 * Criar conta e recuperar senha continuam na página do Cognito (#157 traz o cadastro para o app).
 * Navegador simples, sem esperar código: depois a pessoa entra pela tela do app.
 */
export async function abrirPaginaCognito(pagina: 'signup' | 'forgotPassword'): Promise<void> {
  await WebBrowser.openBrowserAsync(urlPaginaCognito(COGNITO_DOMAIN, COGNITO_CLIENT_ID, pagina));
}
