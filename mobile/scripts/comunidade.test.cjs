const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function pendente() {
  let resolve, reject;
  const promise = new Promise((ok, erro) => {
    resolve = ok;
    reject = erro;
  });
  return { promise, resolve, reject };
}

async function montar({ sigo = false, seguir, sair, curtir, descurtir } = {}) {
  const source = fs.readFileSync(
    path.join(__dirname, '../src/features/comunidade/useComunidade.ts'),
    'utf8'
  );
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  });
  const deps = {
    react: React,
    '@/api/client': { ApiError: Error },
    '@/features/posts/api': {
      curtir:
        curtir ?? (async () => ({ curtidoPorMim: true, totalCurtidas: 7 })),
      descurtir:
        descurtir ?? (async () => ({ curtidoPorMim: false, totalCurtidas: 9 })),
    },
    './api': {
      buscarComunidade: async () => ({
        id: 'comunidade',
        sigo,
        seguidores: 2,
        posts: ['a', 'b'].map((id) => ({
          id,
          curtidoPorMim: false,
          totalCurtidas: 0,
        })),
      }),
      seguirComunidade: seguir ?? (async () => ({ sigo: true, seguidores: 3 })),
      deixarDeSeguirComunidade:
        sair ?? (async () => ({ sigo: false, seguidores: 1 })),
    },
  };
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => deps[name],
    module,
    module.exports
  );
  let hook;
  function Probe() {
    hook = module.exports.useComunidade('comunidade');
    return null;
  }
  let renderer;
  await act(async () => {
    renderer = create(React.createElement(Probe));
  });
  return {
    get hook() {
      return hook;
    },
    fechar: () => act(async () => renderer.unmount()),
  };
}

for (const sigo of [false, true]) {
  test(`falha ao ${sigo ? 'sair' : 'seguir'} preserva curtida concluída`, async () => {
    const chamada = pendente();
    const tela = await montar({
      sigo,
      seguir: () => chamada.promise,
      sair: () => chamada.promise,
    });
    try {
      let seguimento;
      await act(async () => {
        seguimento = tela.hook.alternarSeguimento().catch((erro) => erro);
      });
      await act(async () => {
        await tela.hook.alternarCurtida('a');
      });
      await act(async () => {
        chamada.reject(new Error('Falha'));
        assert.equal((await seguimento).message, 'Falha');
      });
      const dados = tela.hook.comunidade.dados;
      assert.equal(dados.sigo, sigo);
      assert.equal(dados.seguidores, 2);
      assert.equal(dados.posts[0].curtidoPorMim, true);
      assert.equal(dados.posts[0].totalCurtidas, 7);
    } finally {
      await tela.fechar();
    }
  });
}

test('curtir e descurtir aplicam estado e contagem retornados pela API', async () => {
  const tela = await montar();
  try {
    await act(async () => {
      await tela.hook.alternarCurtida('a');
    });
    assert.equal(tela.hook.comunidade.dados.posts[0].totalCurtidas, 7);
    assert.equal(tela.hook.comunidade.dados.posts[0].curtidoPorMim, true);
    await act(async () => {
      await tela.hook.alternarCurtida('a');
    });
    assert.equal(tela.hook.comunidade.dados.posts[0].totalCurtidas, 9);
    assert.equal(tela.hook.comunidade.dados.posts[0].curtidoPorMim, false);
  } finally {
    await tela.fechar();
  }
});

test('curtidas em posts diferentes e seguir no mesmo ciclo preservam todas as alterações', async () => {
  const primeira = pendente(),
    segunda = pendente(),
    seguir = pendente();
  const tela = await montar({
    seguir: () => seguir.promise,
    curtir: (id) => (id === 'a' ? primeira.promise : segunda.promise),
  });
  try {
    let a, b, participacao;
    await act(async () => {
      participacao = tela.hook.alternarSeguimento();
      a = tela.hook.alternarCurtida('a');
      b = tela.hook.alternarCurtida('b');
    });
    assert.equal(tela.hook.comunidade.dados.sigo, true);
    assert.ok(tela.hook.comunidade.dados.posts.every((p) => p.curtidoPorMim));
    await act(async () => {
      segunda.resolve({ curtidoPorMim: true, totalCurtidas: 8 });
      await b;
      seguir.resolve({ sigo: true, seguidores: 5 });
      await participacao;
      primeira.resolve({ curtidoPorMim: true, totalCurtidas: 4 });
      await a;
    });
    assert.equal(tela.hook.comunidade.dados.seguidores, 5);
    assert.deepEqual(
      tela.hook.comunidade.dados.posts.map((p) => p.totalCurtidas),
      [4, 8]
    );
  } finally {
    await tela.fechar();
  }
});

test('falha de uma curtida preserva participação e outro post', async () => {
  const falha = pendente();
  const tela = await montar({
    curtir: (id) =>
      id === 'a'
        ? falha.promise
        : Promise.resolve({ curtidoPorMim: true, totalCurtidas: 6 }),
  });
  try {
    let chamada;
    await act(async () => {
      chamada = tela.hook.alternarCurtida('a').catch((erro) => erro);
    });
    await act(async () => {
      await tela.hook.alternarSeguimento();
      await tela.hook.alternarCurtida('b');
    });
    await act(async () => {
      falha.reject(new Error('Falha'));
      await chamada;
    });
    assert.equal(tela.hook.comunidade.dados.sigo, true);
    assert.deepEqual(
      tela.hook.comunidade.dados.posts.map((p) => p.totalCurtidas),
      [0, 6]
    );
  } finally {
    await tela.fechar();
  }
});
