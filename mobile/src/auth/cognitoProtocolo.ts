/**
 * Conversa com a API do Cognito (login, renovação, saída) sem dependências: só monta requisições,
 * traduz erros e lê respostas. Testado com node:test em tests/cognitoProtocolo.test.mjs; por isso
 * não importa nada do app.
 */

export type TipoErroLogin = 'credenciais' | 'naoConfirmado' | 'muitasTentativas' | 'falha';

export const MENSAGENS_ERRO_LOGIN: Record<TipoErroLogin, string> = {
  credenciais: 'E-mail ou senha incorretos.',
  naoConfirmado:
    'Confirme seu e-mail antes de entrar. Procure o código na sua caixa de entrada.',
  muitasTentativas: 'Muitas tentativas. Espere alguns minutos e tente de novo.',
  falha: 'Não foi possível entrar. Verifique sua conexão e tente de novo.',
};

/** A mensagem sai sempre da tabela acima: nada que o Cognito devolva (nem a senha) vaza. */
export class ErroLogin extends Error {
  readonly tipo: TipoErroLogin;

  constructor(tipo: TipoErroLogin) {
    super(MENSAGENS_ERRO_LOGIN[tipo]);
    this.name = 'ErroLogin';
    this.tipo = tipo;
  }
}

export type SessaoCognito = {
  accessToken: string;
  refreshToken: string | null;
  /** Epoch em ms. */
  expiresAt: number;
};

export type RequisicaoCognito = { headers: Record<string, string>; body: string };

/** Domínio próprio (sem a região no nome) cai aqui. */
export const REGIAO_PADRAO = 'us-east-2';

const REDIRECT_URI = 'readrace://auth';

export function regiaoDoDominio(dominio: string): string {
  const achado = /\.auth\.([a-z0-9-]+)\.amazoncognito\.com/.exec(dominio);
  return achado ? achado[1] : REGIAO_PADRAO;
}

export function endpointCognito(regiao: string): string {
  return `https://cognito-idp.${regiao}.amazonaws.com/`;
}

function requisicao(acao: string, corpo: object): RequisicaoCognito {
  return {
    headers: {
      'Content-Type': 'application/x-amz-json-1.1',
      'X-Amz-Target': `AWSCognitoIdentityProviderService.${acao}`,
    },
    body: JSON.stringify(corpo),
  };
}

/** E-mail aparado: um espaço colado no fim não pode virar "senha incorreta". A senha vai como veio. */
export function requisicaoLogin(clientId: string, email: string, senha: string): RequisicaoCognito {
  return requisicao('InitiateAuth', {
    AuthFlow: 'USER_PASSWORD_AUTH',
    ClientId: clientId,
    AuthParameters: { USERNAME: email.trim(), PASSWORD: senha },
  });
}

export function requisicaoRenovacao(clientId: string, refreshToken: string): RequisicaoCognito {
  return requisicao('InitiateAuth', {
    AuthFlow: 'REFRESH_TOKEN_AUTH',
    ClientId: clientId,
    AuthParameters: { REFRESH_TOKEN: refreshToken },
  });
}

export function requisicaoRevogacao(clientId: string, refreshToken: string): RequisicaoCognito {
  return requisicao('RevokeToken', { ClientId: clientId, Token: refreshToken });
}

/**
 * Página do Cognito aberta num navegador simples. O retorno é sempre readrace://auth: é o único
 * endereço cadastrado no app client; com o endereço do Expo Go (exp://) a página responde 400.
 */
export function urlPaginaCognito(
  dominio: string,
  clientId: string,
  pagina: 'signup' | 'forgotPassword'
): string {
  const parametros = new URLSearchParams({
    client_id: clientId,
    response_type: 'code',
    redirect_uri: REDIRECT_URI,
    lang: 'pt-BR',
  });
  return `${dominio.replace(/\/+$/, '')}/${pagina}?${parametros.toString()}`;
}

/** Conta inexistente e senha errada dão a mesma resposta: não revela quais e-mails têm conta. */
export function traduzirErroLogin(
  tipoAws: string | undefined,
  mensagem: string | undefined
): TipoErroLogin {
  const tipo = (tipoAws ?? '').split('#').pop();
  switch (tipo) {
    case 'NotAuthorizedException':
      return /attempts exceeded/i.test(mensagem ?? '') ? 'muitasTentativas' : 'credenciais';
    case 'UserNotFoundException':
      return 'credenciais';
    case 'UserNotConfirmedException':
      return 'naoConfirmado';
    case 'TooManyRequestsException':
    case 'LimitExceededException':
      return 'muitasTentativas';
    default:
      return 'falha';
  }
}

/**
 * Só sai da conta quem o Cognito recusou. Sem rede ou com limite de chamadas o refresh token
 * continua válido: a sessão fica e a renovação é tentada de novo na próxima chamada.
 */
export function renovacaoEncerraSessao(erro: unknown): boolean {
  if (!(erro instanceof ErroLogin)) {
    return true;
  }
  return erro.tipo !== 'falha' && erro.tipo !== 'muitasTentativas';
}

export async function chamarCognito(
  fetchImpl: typeof fetch,
  endpoint: string,
  req: RequisicaoCognito
): Promise<any> {
  let resposta: Response;
  try {
    resposta = await fetchImpl(endpoint, { method: 'POST', headers: req.headers, body: req.body });
  } catch {
    throw new ErroLogin('falha');
  }
  const corpo = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    throw new ErroLogin(traduzirErroLogin(corpo?.__type, corpo?.message));
  }
  return corpo;
}

/**
 * Sem AuthenticationResult o Cognito pediu um desafio (ex.: trocar a senha), que o app ainda não
 * trata. Na renovação ele não manda refresh token novo: fica o anterior.
 */
export function sessaoDaResposta(
  corpo: any,
  refreshAnterior: string | null,
  agora: number
): SessaoCognito {
  const resultado = corpo?.AuthenticationResult;
  if (!resultado?.AccessToken) {
    throw new ErroLogin('falha');
  }
  return {
    accessToken: resultado.AccessToken,
    refreshToken: resultado.RefreshToken ?? refreshAnterior,
    expiresAt: agora + (resultado.ExpiresIn ?? 3600) * 1000,
  };
}
