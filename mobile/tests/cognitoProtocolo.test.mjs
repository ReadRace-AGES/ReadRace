import assert from 'node:assert/strict';
import test from 'node:test';

import {
  chamarCognito,
  emailValido,
  ErroCadastro,
  erroDeCadastro,
  MENSAGENS_ERRO_CADASTRO,
  nomeValido,
  REGRA_SENHA,
  requisicaoCadastro,
  requisicaoConfirmacao,
  requisicaoReenvio,
  senhaValida,
  soDigitos,
  traduzirErroCadastro,
  endpointCognito,
  ErroLogin,
  MENSAGENS_ERRO_LOGIN,
  regiaoDoDominio,
  renovacaoEncerraSessao,
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

test('renovacaoEncerraSessao encerra quando o Cognito recusa o refresh token', () => {
  assert.equal(renovacaoEncerraSessao(new ErroLogin('credenciais')), true);
});

test('renovacaoEncerraSessao mantém a sessão sem rede ou com limite de chamadas', () => {
  assert.equal(renovacaoEncerraSessao(new ErroLogin('falha')), false);
  assert.equal(renovacaoEncerraSessao(new ErroLogin('muitasTentativas')), false);
});

test('renovacaoEncerraSessao encerra com erro que não veio do Cognito', () => {
  assert.equal(renovacaoEncerraSessao(new Error('Sessão sem refresh token.')), true);
});

test('requisicaoCadastro usa SignUp com nome e e-mail aparados', () => {
  const req = requisicaoCadastro('cli', '  Maria Lopes ', ' maria@exemplo.com ', 'Senha#123');
  assert.equal(req.headers['X-Amz-Target'], 'AWSCognitoIdentityProviderService.SignUp');
  assert.deepEqual(JSON.parse(req.body), {
    ClientId: 'cli',
    Username: 'maria@exemplo.com',
    Password: 'Senha#123',
    UserAttributes: [
      { Name: 'name', Value: 'Maria Lopes' },
      { Name: 'email', Value: 'maria@exemplo.com' },
    ],
  });
});

test('requisicaoConfirmacao usa ConfirmSignUp', () => {
  const req = requisicaoConfirmacao('cli', ' maria@exemplo.com', '482913');
  assert.equal(req.headers['X-Amz-Target'], 'AWSCognitoIdentityProviderService.ConfirmSignUp');
  assert.deepEqual(JSON.parse(req.body), {
    ClientId: 'cli',
    Username: 'maria@exemplo.com',
    ConfirmationCode: '482913',
  });
});

test('requisicaoReenvio usa ResendConfirmationCode', () => {
  const req = requisicaoReenvio('cli', 'maria@exemplo.com ');
  assert.equal(
    req.headers['X-Amz-Target'],
    'AWSCognitoIdentityProviderService.ResendConfirmationCode'
  );
  assert.deepEqual(JSON.parse(req.body), { ClientId: 'cli', Username: 'maria@exemplo.com' });
});

test('nomeValido aceita de 1 a 120 caracteres sem contar as pontas', () => {
  assert.equal(nomeValido(''), false);
  assert.equal(nomeValido('   '), false);
  assert.equal(nomeValido(' A '), true);
  assert.equal(nomeValido('a'.repeat(120)), true);
  assert.equal(nomeValido('a'.repeat(121)), false);
});

test('emailValido exige usuário, arroba e domínio com ponto', () => {
  assert.equal(emailValido('maria@exemplo.com'), true);
  assert.equal(emailValido(' maria@exemplo.com '), true);
  assert.equal(emailValido('maria@exemplo'), false);
  assert.equal(emailValido('maria.exemplo.com'), false);
  assert.equal(emailValido('ma ria@exemplo.com'), false);
});

test('senhaValida segue a política do Cognito', () => {
  assert.equal(senhaValida('Senha#12'), true);
  assert.equal(senhaValida('Senh#12'), false, '7 caracteres');
  assert.equal(senhaValida('senha#123'), false, 'sem maiúscula');
  assert.equal(senhaValida('SENHA#123'), false, 'sem minúscula');
  assert.equal(senhaValida('Senha#abc'), false, 'sem número');
  assert.equal(senhaValida('Senha1234'), false, 'sem símbolo');
});

test('soDigitos descarta o que não for número', () => {
  assert.equal(soDigitos('482 913'), '482913');
  assert.equal(soDigitos('482-913\n'), '482913');
  assert.equal(soDigitos('abc'), '');
});

test('traduzirErroCadastro separa limite de e-mails de tentativas demais', () => {
  assert.equal(traduzirErroCadastro('LimitExceededException', 'cadastro'), 'limiteEmails');
  assert.equal(traduzirErroCadastro('LimitExceededException', 'reenvio'), 'limiteEmails');
  assert.equal(traduzirErroCadastro('LimitExceededException', 'confirmacao'), 'muitasTentativas');
  assert.equal(traduzirErroCadastro('CodeDeliveryFailureException', 'reenvio'), 'limiteEmails');
  assert.equal(traduzirErroCadastro('TooManyRequestsException', 'cadastro'), 'limiteEmails');
});

test('traduzirErroCadastro cobre os casos da issue', () => {
  assert.equal(traduzirErroCadastro('UsernameExistsException', 'cadastro'), 'emailJaExiste');
  assert.equal(traduzirErroCadastro('InvalidPasswordException', 'cadastro'), 'senhaFraca');
  assert.equal(traduzirErroCadastro('CodeMismatchException', 'confirmacao'), 'codigoIncorreto');
  assert.equal(traduzirErroCadastro('ExpiredCodeException', 'confirmacao'), 'codigoExpirado');
  assert.equal(
    traduzirErroCadastro(
      'NotAuthorizedException',
      'confirmacao',
      'User cannot be confirmed. Current status is CONFIRMED'
    ),
    'jaConfirmada'
  );
  assert.equal(traduzirErroCadastro('NotAuthorizedException', 'cadastro'), 'falha');
  assert.equal(
    traduzirErroCadastro('com.amazonaws#UsernameExistsException', 'cadastro'),
    'emailJaExiste'
  );
  assert.equal(traduzirErroCadastro(undefined, 'cadastro'), 'falha');
});

test('traduzirErroCadastro só trata como já confirmada quando o Cognito diz isso', () => {
  assert.equal(
    traduzirErroCadastro('NotAuthorizedException', 'confirmacao', 'User is disabled.'),
    'falha'
  );
  assert.equal(traduzirErroCadastro('NotAuthorizedException', 'confirmacao'), 'falha');
  assert.equal(
    traduzirErroCadastro('InvalidParameterException', 'reenvio', 'User is already confirmed.'),
    'jaConfirmada'
  );
  assert.equal(
    traduzirErroCadastro('InvalidParameterException', 'reenvio', 'Username should be an email.'),
    'falha'
  );
});

test('traduzirErroCadastro trata código errado vezes demais como tentativas demais', () => {
  assert.equal(
    traduzirErroCadastro('TooManyFailedAttemptsException', 'confirmacao'),
    'muitasTentativas'
  );
});

test('ErroCadastro carrega a mensagem da issue', () => {
  assert.equal(
    new ErroCadastro('emailJaExiste').message,
    'Já existe uma conta com este e-mail. Entre ou recupere a senha.'
  );
  assert.equal(new ErroCadastro('senhaFraca').message, REGRA_SENHA);
  assert.equal(
    MENSAGENS_ERRO_CADASTRO.falha,
    'Não foi possível criar a conta. Verifique sua conexão e tente de novo.'
  );
});

test('chamarCognito com erroDeCadastro traduz pela ação', async () => {
  await assert.rejects(
    chamarCognito(
      respostaFalsa(400, { __type: 'CodeMismatchException', message: 'Invalid code' }),
      'https://x/',
      requisicaoConfirmacao('cli', 'a@b.com', '1'),
      erroDeCadastro('confirmacao')
    ),
    (erro) => erro instanceof ErroCadastro && erro.tipo === 'codigoIncorreto'
  );
});

test('chamarCognito com erroDeCadastro sem rede vira falha do cadastro', async () => {
  const semRede = async () => {
    throw new TypeError('Network request failed');
  };
  await assert.rejects(
    chamarCognito(semRede, 'https://x/', requisicaoReenvio('c', 'a'), erroDeCadastro('reenvio')),
    (erro) => erro instanceof ErroCadastro && erro.tipo === 'falha'
  );
});

test('ErroCadastro não carrega a senha', async () => {
  const senha = 'SenhaSuperSecreta#123';
  try {
    await chamarCognito(
      respostaFalsa(400, { __type: 'InvalidPasswordException', message: `bad ${senha}` }),
      'https://x/',
      requisicaoCadastro('cli', 'Maria', 'a@b.com', senha),
      erroDeCadastro('cadastro')
    );
    assert.fail('deveria ter lançado');
  } catch (erro) {
    assert.ok(!String(erro.message).includes(senha));
    assert.ok(!JSON.stringify(erro).includes(senha));
  }
});
