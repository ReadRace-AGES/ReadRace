import * as WebBrowser from 'expo-web-browser';

import {
  chamarCognito,
  erroDeCadastro,
  requisicaoCadastro,
  requisicaoConfirmacao,
  requisicaoReenvio,
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
 * Criar conta e recuperar senha continuam na página do Cognito (#157 traz o cadastro para o app).
 * Navegador simples, sem esperar código: depois a pessoa entra pela tela do app.
 */
export async function abrirPaginaCognito(pagina: 'signup' | 'forgotPassword'): Promise<void> {
  await WebBrowser.openBrowserAsync(urlPaginaCognito(COGNITO_DOMAIN, COGNITO_CLIENT_ID, pagina));
}
