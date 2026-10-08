import assert from 'node:assert/strict';
import test from 'node:test';

import {
  chamarCognito,
  endpointCognito,
  ErroLogin,
  MENSAGENS_ERRO_LOGIN,
  regiaoDoDominio,
  requisicaoLogin,
  requisicaoRenovacao,
  requisicaoRevogacao,
  sessaoDaResposta,
  traduzirErroLogin,
  urlPaginaCognito,
} from '../src/auth/cognitoProtocolo.ts';

const DOMINIO = 'https://us-east-2mevipejhy.auth.us-east-2.amazoncognito.com';

test('regiaoDoDominio lê a região do domínio do Cognito', () => {
  assert.equal(regiaoDoDominio(DOMINIO), 'us-east-2');
  assert.equal(regiaoDoDominio('https://x.auth.sa-east-1.amazoncognito.com/'), 'sa-east-1');
});

test('regiaoDoDominio usa us-east-2 com domínio próprio ou vazio', () => {
  assert.equal(regiaoDoDominio('https://login.readrace.com'), 'us-east-2');
  assert.equal(regiaoDoDominio(''), 'us-east-2');
});

test('endpointCognito monta o endereço da API da região', () => {
  assert.equal(endpointCognito('us-east-2'), 'https://cognito-idp.us-east-2.amazonaws.com/');
});

test('requisicaoLogin usa USER_PASSWORD_AUTH e o cabeçalho do InitiateAuth', () => {
  const req = requisicaoLogin('cli', 'a@b.com', 'segredo');
  assert.equal(req.headers['X-Amz-Target'], 'AWSCognitoIdentityProviderService.InitiateAuth');
  assert.equal(req.headers['Content-Type'], 'application/x-amz-json-1.1');
  assert.deepEqual(JSON.parse(req.body), {
    AuthFlow: 'USER_PASSWORD_AUTH',
    ClientId: 'cli',
    AuthParameters: { USERNAME: 'a@b.com', PASSWORD: 'segredo' },
  });
});

test('requisicaoLogin apara o e-mail e não mexe na senha', () => {
  const corpo = JSON.parse(requisicaoLogin('cli', '  a@b.com \n', ' com espaço ').body);
  assert.equal(corpo.AuthParameters.USERNAME, 'a@b.com');
  assert.equal(corpo.AuthParameters.PASSWORD, ' com espaço ');
});

test('requisicaoRenovacao usa REFRESH_TOKEN_AUTH', () => {
  assert.deepEqual(JSON.parse(requisicaoRenovacao('cli', 'rt').body), {
    AuthFlow: 'REFRESH_TOKEN_AUTH',
    ClientId: 'cli',
    AuthParameters: { REFRESH_TOKEN: 'rt' },
  });
});

test('requisicaoRevogacao usa RevokeToken', () => {
  const req = requisicaoRevogacao('cli', 'rt');
  assert.equal(req.headers['X-Amz-Target'], 'AWSCognitoIdentityProviderService.RevokeToken');
  assert.deepEqual(JSON.parse(req.body), { ClientId: 'cli', Token: 'rt' });
});

test('urlPaginaCognito usa readrace://auth, code e pt-BR', () => {
  for (const pagina of ['signup', 'forgotPassword']) {
    const url = new URL(urlPaginaCognito(DOMINIO + '/', 'cli', pagina));
    assert.equal(url.origin + url.pathname, `${DOMINIO}/${pagina}`);
    assert.equal(url.searchParams.get('client_id'), 'cli');
    assert.equal(url.searchParams.get('response_type'), 'code');
    assert.equal(url.searchParams.get('redirect_uri'), 'readrace://auth');
    assert.equal(url.searchParams.get('lang'), 'pt-BR');
  }
});

test('traduzirErroLogin cobre os casos da issue', () => {
  assert.equal(traduzirErroLogin('NotAuthorizedException', 'Incorrect username or password.'), 'credenciais');
  assert.equal(traduzirErroLogin('UserNotFoundException', 'User does not exist.'), 'credenciais');
  assert.equal(traduzirErroLogin('UserNotConfirmedException', ''), 'naoConfirmado');
  assert.equal(traduzirErroLogin('NotAuthorizedException', 'Password attempts exceeded'), 'muitasTentativas');
  assert.equal(traduzirErroLogin('TooManyRequestsException', ''), 'muitasTentativas');
  assert.equal(traduzirErroLogin('LimitExceededException', ''), 'muitasTentativas');
  assert.equal(traduzirErroLogin('InternalErrorException', ''), 'falha');
  assert.equal(traduzirErroLogin(undefined, undefined), 'falha');
});

test('traduzirErroLogin aceita prefixo de namespace', () => {
  assert.equal(
    traduzirErroLogin('com.amazonaws.cognito#UserNotConfirmedException', ''),
    'naoConfirmado'
  );
});

test('ErroLogin carrega a mensagem da issue', () => {
  const erro = new ErroLogin('credenciais');
  assert.equal(erro.tipo, 'credenciais');
  assert.equal(erro.message, MENSAGENS_ERRO_LOGIN.credenciais);
  assert.ok(erro instanceof Error);
});

function respostaFalsa(status, corpo) {
  return async () =>
    new Response(typeof corpo === 'string' ? corpo : JSON.stringify(corpo), { status });
}

test('chamarCognito devolve o corpo no sucesso', async () => {
  const corpo = await chamarCognito(
    respostaFalsa(200, { AuthenticationResult: { AccessToken: 'at' } }),
    'https://x/',
    requisicaoLogin('cli', 'a@b.com', 's')
  );
  assert.equal(corpo.AuthenticationResult.AccessToken, 'at');
});

test('chamarCognito traduz o erro do Cognito', async () => {
  await assert.rejects(
    chamarCognito(
      respostaFalsa(400, { __type: 'UserNotConfirmedException', message: 'User is not confirmed.' }),
      'https://x/',
      requisicaoLogin('cli', 'a@b.com', 's')
    ),
    (erro) => erro instanceof ErroLogin && erro.tipo === 'naoConfirmado'
  );
});

test('chamarCognito com corpo inválido vira falha', async () => {
  await assert.rejects(
    chamarCognito(respostaFalsa(502, '<html>Bad Gateway</html>'), 'https://x/', requisicaoLogin('c', 'a', 's')),
    (erro) => erro instanceof ErroLogin && erro.tipo === 'falha'
  );
});

test('chamarCognito sem rede vira falha', async () => {
  const semRede = async () => {
    throw new TypeError('Network request failed');
  };
  await assert.rejects(
    chamarCognito(semRede, 'https://x/', requisicaoLogin('c', 'a', 's')),
    (erro) => erro instanceof ErroLogin && erro.tipo === 'falha'
  );
});

test('ErroLogin não carrega a senha', async () => {
  const senha = 'SenhaSuperSecreta#123';
  try {
    await chamarCognito(
      respostaFalsa(400, { __type: 'NotAuthorizedException', message: `bad ${senha}` }),
      'https://x/',
      requisicaoLogin('cli', 'a@b.com', senha)
    );
    assert.fail('deveria ter lançado');
  } catch (erro) {
    assert.ok(!String(erro.message).includes(senha));
    assert.ok(!JSON.stringify(erro).includes(senha));
  }
});

test('sessaoDaResposta calcula a expiração e guarda o refresh token', () => {
  const sessao = sessaoDaResposta(
    { AuthenticationResult: { AccessToken: 'at', RefreshToken: 'rt', ExpiresIn: 3600 } },
    null,
    1_000
  );
  assert.deepEqual(sessao, { accessToken: 'at', refreshToken: 'rt', expiresAt: 3_601_000 });
});

test('sessaoDaResposta mantém o refresh anterior na renovação', () => {
  const sessao = sessaoDaResposta({ AuthenticationResult: { AccessToken: 'at2', ExpiresIn: 60 } }, 'rt', 0);
  assert.equal(sessao.refreshToken, 'rt');
  assert.equal(sessao.expiresAt, 60_000);
});

test('sessaoDaResposta recusa desafio', () => {
  assert.throws(
    () => sessaoDaResposta({ ChallengeName: 'NEW_PASSWORD_REQUIRED', Session: 's' }, null, 0),
    (erro) => erro instanceof ErroLogin && erro.tipo === 'falha'
  );
});
