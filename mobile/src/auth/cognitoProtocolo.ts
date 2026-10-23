/**
 * Conversa com a API do Cognito (login, renovação, saída e cadastro) sem dependências: só monta
 * requisições, valida campos, traduz erros e lê respostas. Testado com node:test em
 * tests/cognitoProtocolo.test.mjs; por isso não importa nada do app.
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

/** Nome e e-mail aparados. O e-mail é o usuário do pool (username_attributes = email). */
export function requisicaoCadastro(
  clientId: string,
  nome: string,
  email: string,
  senha: string
): RequisicaoCognito {
  const emailLimpo = email.trim();
  return requisicao('SignUp', {
    ClientId: clientId,
    Username: emailLimpo,
    Password: senha,
    UserAttributes: [
      { Name: 'name', Value: nome.trim() },
      { Name: 'email', Value: emailLimpo },
    ],
  });
}

export function requisicaoConfirmacao(
  clientId: string,
  email: string,
  codigo: string
): RequisicaoCognito {
  return requisicao('ConfirmSignUp', {
    ClientId: clientId,
    Username: email.trim(),
    ConfirmationCode: codigo,
  });
}

export function requisicaoReenvio(clientId: string, email: string): RequisicaoCognito {
  return requisicao('ResendConfirmationCode', { ClientId: clientId, Username: email.trim() });
}

/** Coluna usuario.nome é VARCHAR(120): o primeiro acesso grava o nome do Cognito nela. */
export const TAMANHO_MAX_NOME = 120;

/** Política de senha do pool (infra/terraform/cognito.tf). Quem decide é o Cognito. */
export const REGRA_SENHA =
  'Mínimo de 8 caracteres, com letra maiúscula, minúscula, número e símbolo.';

export function nomeValido(nome: string): boolean {
  const limpo = nome.trim();
  return limpo.length >= 1 && limpo.length <= TAMANHO_MAX_NOME;
}

export function emailValido(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}

export function senhaValida(senha: string): boolean {
  return (
    senha.length >= 8 &&
    /[a-z]/.test(senha) &&
    /[A-Z]/.test(senha) &&
    /[0-9]/.test(senha) &&
    /[^A-Za-z0-9]/.test(senha)
  );
}

/** Código colado do e-mail pode vir com espaço ou quebra de linha. */
export function soDigitos(texto: string): string {
  return texto.replace(/\D/g, '');
}

export type AcaoCadastro = 'cadastro' | 'reenvio' | 'confirmacao';

export type TipoErroCadastro =
  | 'emailJaExiste'
  | 'senhaFraca'
  | 'codigoIncorreto'
  | 'codigoExpirado'
  | 'limiteEmails'
  | 'muitasTentativas'
  | 'jaConfirmada'
  | 'falha';

export const MENSAGENS_ERRO_CADASTRO: Record<TipoErroCadastro, string> = {
  emailJaExiste: 'Já existe uma conta com este e-mail. Entre ou recupere a senha.',
  senhaFraca: REGRA_SENHA,
  codigoIncorreto: 'Código incorreto. Confira o e-mail e tente de novo.',
  codigoExpirado: 'O código expirou. Toque em Reenviar código.',
  limiteEmails: 'Não foi possível enviar o e-mail agora. Tente de novo mais tarde.',
  muitasTentativas: MENSAGENS_ERRO_LOGIN.muitasTentativas,
  jaConfirmada: 'Esta conta já foi confirmada.',
  falha: 'Não foi possível criar a conta. Verifique sua conexão e tente de novo.',
};

/** Como o ErroLogin: a mensagem sai sempre da tabela, nada que o Cognito devolva vaza. */
export class ErroCadastro extends Error {
  readonly tipo: TipoErroCadastro;

  constructor(tipo: TipoErroCadastro) {
    super(MENSAGENS_ERRO_CADASTRO[tipo]);
    this.name = 'ErroCadastro';
    this.tipo = tipo;
  }
}

/**
 * O mesmo erro do Cognito muda de sentido com a ação: LimitExceededException é limite de e-mails no
 * cadastro e no reenvio, e tentativas demais na confirmação. NotAuthorizedException na confirmação
 * é conta já confirmada.
 */
export function traduzirErroCadastro(
  tipoAws: string | undefined,
  acao: AcaoCadastro,
  mensagem?: string
): TipoErroCadastro {
  const tipo = (tipoAws ?? '').split('#').pop();
  // O mesmo tipo do Cognito cobre outros motivos (ex.: conta desativada): só a mensagem distingue
  // a conta já confirmada.
  const jaConfirmada = /already confirmed|status is CONFIRMED/i.test(mensagem ?? '');
  switch (tipo) {
    case 'UsernameExistsException':
      return acao === 'cadastro' ? 'emailJaExiste' : 'falha';
    case 'InvalidPasswordException':
      return 'senhaFraca';
    case 'CodeMismatchException':
      return 'codigoIncorreto';
    case 'ExpiredCodeException':
      return 'codigoExpirado';
    case 'TooManyFailedAttemptsException':
      return 'muitasTentativas';
    case 'LimitExceededException':
    case 'CodeDeliveryFailureException':
    case 'TooManyRequestsException':
      return acao === 'confirmacao' ? 'muitasTentativas' : 'limiteEmails';
    case 'NotAuthorizedException':
      return acao === 'confirmacao' && jaConfirmada ? 'jaConfirmada' : 'falha';
    case 'InvalidParameterException':
      return acao === 'reenvio' && jaConfirmada ? 'jaConfirmada' : 'falha';
    default:
      return 'falha';
  }
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

/** Monta o erro lançado pelo chamarCognito. Sem rede, recebe tipo e mensagem indefinidos. */
export type ErroDaResposta = (tipoAws: string | undefined, mensagem: string | undefined) => Error;

const erroDeLogin: ErroDaResposta = (tipoAws, mensagem) =>
  new ErroLogin(traduzirErroLogin(tipoAws, mensagem));

export function erroDeCadastro(acao: AcaoCadastro): ErroDaResposta {
  return (tipoAws, mensagem) => new ErroCadastro(traduzirErroCadastro(tipoAws, acao, mensagem));
}

export async function chamarCognito(
  fetchImpl: typeof fetch,
  endpoint: string,
  req: RequisicaoCognito,
  erroDaResposta: ErroDaResposta = erroDeLogin
): Promise<any> {
  let resposta: Response;
  try {
    resposta = await fetchImpl(endpoint, { method: 'POST', headers: req.headers, body: req.body });
  } catch {
    throw erroDaResposta(undefined, undefined);
  }
  const corpo = await resposta.json().catch(() => ({}));
  if (!resposta.ok) {
    throw erroDaResposta(corpo?.__type, corpo?.message);
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

export const MENSAGEM_ERRO_GOOGLE = 'Não foi possível entrar com o Google. Tente de novo.';

/**
 * Escopos pedidos no login pela página do Cognito. aws.cognito.signin.user.admin é o que deixa a API
 * ler o perfil pelo GetUser no primeiro acesso: sem ele, ela responde 401 e o app perde a sessão
 * logo depois de entrar.
 */
export const ESCOPOS_LOGIN_GOOGLE = ['openid', 'email', 'profile', 'aws.cognito.signin.user.admin'];

export type ResultadoGoogle =
  | { tipo: 'codigo'; codigo: string }
  | { tipo: 'desistiu' }
  | { tipo: 'erro' };

/**
 * Como a página do Cognito voltou. Fechar o navegador e tocar em "Cancelar" na escolha de conta do
 * Google (access_denied) são desistência: a tela volta sem mensagem de erro.
 */
export function resultadoDoLoginGoogle(
  tipo: string,
  params: Record<string, string> | undefined
): ResultadoGoogle {
  if (tipo === 'success' && params?.code) {
    return { tipo: 'codigo', codigo: params.code };
  }
  if (tipo === 'cancel' || tipo === 'dismiss') {
    return { tipo: 'desistiu' };
  }
  if (tipo === 'error' && params?.error === 'access_denied') {
    return { tipo: 'desistiu' };
  }
  return { tipo: 'erro' };
}

/** Token que volta do /oauth2/token, já lido pelo expo-auth-session. */
export type TokenOAuth = {
  accessToken?: string;
  refreshToken?: string;
  expiresIn?: number;
};

/** O login pelo Google rende a mesma sessão do login por senha: a renovação e a saída não mudam. */
export function sessaoDoTokenOAuth(token: TokenOAuth, agora: number): SessaoCognito {
  if (!token?.accessToken) {
    throw new Error('Resposta do Cognito sem access token.');
  }
  return {
    accessToken: token.accessToken,
    refreshToken: token.refreshToken ?? null,
    expiresAt: agora + (token.expiresIn ?? 3600) * 1000,
  };
}

/** User pool do projeto (us-east-2). Valores públicos: vão dentro do app e o client não tem segredo. */
const DOMINIO_PADRAO = 'https://us-east-2mevipejhy.auth.us-east-2.amazoncognito.com';
const CLIENT_ID_PADRAO = '5f06kagodbku0mvveakmsim8qu';

/**
 * O login é o padrão (#159): sem a variável no .env, vale o pool do projeto, e um APK gerado sem
 * .env já vem com login. Variável vazia desliga o login, para quem roda a API em AUTH_MODO=seed.
 */
export function configCognito(
  dominio: string | undefined,
  clientId: string | undefined
): { dominio: string; clientId: string; habilitada: boolean } {
  const dominioFinal = (dominio ?? DOMINIO_PADRAO).replace(/\/+$/, '');
  const clientIdFinal = clientId ?? CLIENT_ID_PADRAO;
  return {
    dominio: dominioFinal,
    clientId: clientIdFinal,
    habilitada: dominioFinal !== '' && clientIdFinal !== '',
  };
}
