import assert from 'node:assert/strict';
import test from 'node:test';

import {
  cadastroPendente,
  ehOMesmoCadastro,
  guardarCadastro,
  limparCadastro,
  registrarReenvio,
  segundosParaReenviar,
} from '../src/auth/cadastroPendente.ts';

test('guarda e limpa o cadastro pendente', () => {
  limparCadastro();
  assert.equal(cadastroPendente(), null);
  guardarCadastro(' maria@exemplo.com ', 'Senha#123', 1000);
  assert.deepEqual(cadastroPendente(), {
    email: 'maria@exemplo.com',
    senha: 'Senha#123',
    ultimoEnvio: 1000,
  });
  limparCadastro();
  assert.equal(cadastroPendente(), null);
});

test('ehOMesmoCadastro ignora pontas e maiúsculas do e-mail, não da senha', () => {
  guardarCadastro('maria@exemplo.com', 'Senha#123', null);
  assert.equal(ehOMesmoCadastro(' Maria@Exemplo.com ', 'Senha#123'), true);
  assert.equal(ehOMesmoCadastro('maria@exemplo.com', 'senha#123'), false);
  assert.equal(ehOMesmoCadastro('outra@exemplo.com', 'Senha#123'), false);
  limparCadastro();
  assert.equal(ehOMesmoCadastro('maria@exemplo.com', 'Senha#123'), false);
});

test('registrarReenvio atualiza o último envio', () => {
  guardarCadastro('maria@exemplo.com', 'Senha#123', null);
  registrarReenvio(5000);
  assert.equal(cadastroPendente()?.ultimoEnvio, 5000);
  limparCadastro();
});

test('segundosParaReenviar conta 60 s a partir do envio', () => {
  assert.equal(segundosParaReenviar(null, 10_000), 0);
  assert.equal(segundosParaReenviar(10_000, 10_000), 60);
  assert.equal(segundosParaReenviar(10_000, 10_500), 60);
  assert.equal(segundosParaReenviar(10_000, 69_001), 1);
  assert.equal(segundosParaReenviar(10_000, 70_000), 0);
  assert.equal(segundosParaReenviar(10_000, 999_999), 0);
});

// O login de conta não confirmada guarda sem envio (null). Se for o cadastro que acabou de mandar
// o código, a espera de 60 s continua: senão o "Reenviar" libera na hora e gasta a cota.
test('guardarCadastro sem envio preserva a espera do mesmo cadastro', () => {
  guardarCadastro('maria@exemplo.com', 'Senha#123', 10_000);
  guardarCadastro(' Maria@Exemplo.com', 'Senha#123', null);
  assert.equal(cadastroPendente()?.ultimoEnvio, 10_000);
  guardarCadastro('outra@exemplo.com', 'Senha#123', null);
  assert.equal(cadastroPendente()?.ultimoEnvio, null);
  limparCadastro();
});
