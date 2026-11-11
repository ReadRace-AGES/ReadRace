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

async function montar({
  sigo = false,
  seguir,
  sair,
  curtir,
  descurtir,
  ordemInicial = 'recentes',
  buscar = async () => ({
    id: 'comunidade',
    sigo,
    seguidores: 2,
    posts: ['a', 'b'].map((id) => ({
      id,
      curtidoPorMim: false,
      totalCurtidas: 0,
    })),
  }),
} = {}) {
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
    '@/api/client': {
      ApiError: Error,
      mensagemErroCarregamento: () => 'Erro',
    },
    '@/features/posts/api': {
      curtir:
        curtir ?? (async () => ({ curtidoPorMim: true, totalCurtidas: 7 })),
      descurtir:
        descurtir ?? (async () => ({ curtidoPorMim: false, totalCurtidas: 9 })),
    },
    './api': {
      buscarComunidade: buscar,
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
  let mudarOrdem;
  function Probe() {
    const [ordem, setOrdem] = React.useState(ordemInicial);
    mudarOrdem = setOrdem;
    hook = module.exports.useComunidade('comunidade', ordem);
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
    mudarOrdem,
    fechar: () => act(async () => renderer.unmount()),
  };
}

test('resposta atrasada preserva curtida e seguimento confirmados depois da consulta', async () => {
  const consulta = pendente();
  const seguir = pendente();
  const curtir = pendente();
  let chamadas = 0;
  const tela = await montar({
    buscar: () => {
      chamadas++;
      return chamadas === 1
        ? Promise.resolve({
            id: 'comunidade',
            sigo: false,
            seguidores: 2,
            posts: [
              { id: 'a', curtidoPorMim: false, totalCurtidas: 0 },
              { id: 'b', curtidoPorMim: false, totalCurtidas: 0 },
            ],
          })
        : consulta.promise;
    },
    seguir: () => seguir.promise,
    curtir: () => curtir.promise,
  });

  try {
    await act(async () => tela.hook.recarregar());
    let participacao, curtida;
    await act(async () => {
      participacao = tela.hook.alternarSeguimento();
      curtida = tela.hook.alternarCurtida('a');
      seguir.resolve({ sigo: true, seguidores: 3 });
      curtir.resolve({ curtidoPorMim: true, totalCurtidas: 1 });
      await Promise.all([participacao, curtida]);
    });
    await act(async () => {
      consulta.resolve({
        id: 'comunidade',
        sigo: false,
        seguidores: 2,
        posts: [
          { id: 'a', curtidoPorMim: false, totalCurtidas: 0 },
          { id: 'b', curtidoPorMim: false, totalCurtidas: 0 },
        ],
      });
    });
    assert.equal(tela.hook.comunidade.dados.sigo, true);
    assert.equal(tela.hook.comunidade.dados.seguidores, 3);
    assert.equal(tela.hook.comunidade.dados.posts[0].curtidoPorMim, true);
    assert.equal(tela.hook.comunidade.dados.posts[0].totalCurtidas, 1);
  } finally {
    await tela.fechar();
  }
});

test('resposta de uma aba antiga nao substitui a ordem atualmente selecionada', async () => {
  const alta = pendente();
  let chamadas = 0;
  const tela = await montar({
    buscar: () => {
      chamadas++;
      if (chamadas === 2) return alta.promise;
      return Promise.resolve({
        id: 'comunidade',
        sigo: false,
        seguidores: 2,
        posts: [
          {
            id: chamadas === 1 ? 'recente' : 'atual',
            curtidoPorMim: false,
            totalCurtidas: 0,
          },
        ],
      });
    },
  });
  try {
    await act(async () => tela.mudarOrdem('em_alta'));
    await act(async () => tela.mudarOrdem('recentes'));
    assert.equal(tela.hook.comunidade.dados.posts[0].id, 'atual');
    await act(async () =>
      alta.resolve({
        id: 'comunidade',
        sigo: false,
        seguidores: 2,
        posts: [
          { id: 'antigo-em-alta', curtidoPorMim: false, totalCurtidas: 10 },
        ],
      })
    );
    assert.equal(tela.hook.comunidade.dados.posts[0].id, 'atual');
  } finally {
    await tela.fechar();
  }
});

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

test('curtir atualiza a contagem sem reordenar a lista imediatamente', async () => {
  const tela = await montar({
    buscar: async () => ({
      id: 'comunidade',
      sigo: false,
      seguidores: 2,
      posts: [
        { id: 'a', curtidoPorMim: false, totalCurtidas: 0 },
        { id: 'b', curtidoPorMim: false, totalCurtidas: 8 },
      ],
    }),
  });
  try {
    await act(async () => tela.hook.alternarCurtida('a'));
    assert.deepEqual(
      tela.hook.comunidade.dados.posts.map((post) => post.id),
      ['a', 'b']
    );
    assert.equal(tela.hook.comunidade.dados.posts[0].totalCurtidas, 7);
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
