const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
function load(file, deps = {}) {
  const { outputText } = ts.transpileModule(
    fs.readFileSync(path.join(__dirname, '../src', file), 'utf8'),
    {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        jsx: ts.JsxEmit.ReactJSX,
      },
    }
  );
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => deps[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}
function deferred() {
  let resolve, reject;
  const promise = new Promise((a, b) => {
    resolve = a;
    reject = b;
  });
  return { promise, resolve, reject };
}
// toJSON omite os hosts ocultos pelo Activity, sem confundir preservação de estado com acesso.
function visiveis(tree, tipo) {
  const encontrados = [];
  function visitar(no) {
    if (Array.isArray(no)) return no.forEach(visitar);
    if (!no || typeof no !== 'object') return;
    if (no.type === tipo) encontrados.push(no);
    no.children?.forEach(visitar);
  }
  visitar(tree.toJSON());
  return encontrados;
}
const client = load('api/client.ts');
function focusRouter() {
  const effects = new Map(),
    navigations = [];
  const router = {
    push: (route) => navigations.push(['push', route]),
    replace: (route) => navigations.push(['replace', route]),
    back: () => navigations.push(['back']),
    canGoBack: () => true,
  };
  return {
    useRouter: () => router,
    useFocusEffect: (cb) =>
      React.useEffect(() => {
        effects.set(cb, cb());
        return () => {
          effects.get(cb)?.();
          effects.delete(cb);
        };
      }, [cb]),
    blur: () => {
      for (const [cb, cleanup] of effects) {
        cleanup?.();
        effects.set(cb, undefined);
      }
    },
    focus: () => {
      for (const [cb, cleanup] of effects) {
        cleanup?.();
        effects.set(cb, cb());
      }
    },
    navigations,
  };
}
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (s) => s },
};
const theme = { ...require('../src/theme/tokens'), textStyles: {} };
const club = (cargo = 'administrador', count = 0) => ({
  id: 'a',
  nome: 'Clube',
  situacao: cargo ? 'membro' : 'nenhuma',
  meuCargo: cargo,
  solicitacoesPendentes: count,
  livroAtual: { id: 'livro', titulo: 'Livro', autor: null },
  ranking: [],
  proximoLider: { id: 'b', nome: 'Ana', avatarUrl: null },
  temQuizAtivo: true,
});
const pedido = (id = 'p1', nomeUsuario = 'ana') => ({
  id,
  usuario: { id: `u-${id}`, nome: `Nome ${id}`, nomeUsuario, avatarUrl: null },
  criadaEm: new Date(2026, 9, 2, 12).toISOString(),
});
const buttons = (tree) => tree.root.findAllByType('PrimaryButton');
const button = (tree, label) =>
  buttons(tree).find((n) => n.props.label === label);
function ui(router, toasts = []) {
  return {
    'expo-router': router,
    'react-native': native,
    '@/api/client': client,
    '@/theme': theme,
    'react-native-svg': {
      __esModule: true,
      default: 'Svg',
      Path: 'Path',
      Rect: 'Rect',
    },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/EmptyState': {
      EmptyState: ({ action, ...props }) =>
        React.createElement('EmptyState', props, action),
    },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/Card': {
      Card: ({ children, ...props }) =>
        React.createElement('Card', props, children),
    },
    '@/components/BottomSheet': {
      BottomSheet: ({ children, ...props }) =>
        React.createElement('BottomSheet', props, children),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showErrorToast: feedback,
        showToastAfterNavigation: after,
        showToast: () => {},
      }),
    },
  };
  function feedback(message) {
    toasts.push(['feedback', message]);
  }
  function after(message) {
    toasts.push(['navigation', message]);
  }
}
function harness() {
  const router = focusRouter(),
    gets = [],
    posts = [],
    feedback = [],
    denials = [];
  const onFeedback = (message) => feedback.push(message),
    onSemAcesso = (message) => denials.push(message);
  const api = {
    buscarSolicitacoes: (id, signal) => {
      const d = deferred();
      gets.push({ ...d, id, signal });
      return d.promise;
    },
    responderSolicitacao: (clubeId, id, resposta, signal) => {
      const d = deferred();
      posts.push({ ...d, clubeId, id, resposta, signal });
      return d.promise;
    },
  };
  const { useSolicitacoesClube } = load(
    'features/clube/useSolicitacoesClube.ts',
    { 'expo-router': router, '@/api/client': client, './api': api }
  );
  let current, tree;
  const frames = [];
  function Probe({ id }) {
    current = useSolicitacoesClube(id, { onFeedback, onSemAcesso });
    frames.push(current);
    return null;
  }
  const render = (id) => React.createElement(Probe, { id });
  return {
    router,
    gets,
    posts,
    feedback,
    denials,
    frames,
    api,
    get state() {
      return current;
    },
    mount: () =>
      act(() => {
        tree = create(render('a'));
      }),
    change: (id) => act(() => tree.update(render(id))),
    unmount: () => act(() => tree.unmount()),
    resolveList: (index, solicitacoes) =>
      act(async () => gets[index].resolve({ solicitacoes })),
    respond: (id, resposta) =>
      act(() => {
        void current.responder(id, resposta);
      }),
  };
}

test('contratos do cliente: GET e POST, IDs codificados, sem corpo com associação/XP e sem ordenação local', async () => {
  const calls = [],
    solicitacoes = [pedido('z'), pedido('a')];
  const api = load('features/clube/api.ts', {
    '@/api/client': {
      apiGet: async (...args) => {
        calls.push(args);
        return { solicitacoes };
      },
      apiRequest: async (...args) => {
        calls.push(args);
        return {
          id: 'p/1',
          situacao: args[0].endsWith('aceitar') ? 'aceita' : 'recusada',
        };
      },
    },
  });
  const signal = new AbortController().signal;
  assert.deepEqual(await api.buscarSolicitacoes('a/b', signal), {
    solicitacoes,
  });
  assert.deepEqual(
    await api.responderSolicitacao('a/b', 'p/1', 'aceitar', signal),
    { id: 'p/1', situacao: 'aceita' }
  );
  assert.deepEqual(
    await api.responderSolicitacao('a/b', 'p/1', 'recusar', signal),
    { id: 'p/1', situacao: 'recusada' }
  );
  assert.deepEqual(calls, [
    ['/api/clubes/a%2Fb/solicitacoes', { signal }],
    [
      '/api/clubes/a%2Fb/solicitacoes/p%2F1/aceitar',
      { method: 'POST', signal },
    ],
    [
      '/api/clubes/a%2Fb/solicitacoes/p%2F1/recusar',
      { method: 'POST', signal },
    ],
  ]);
});

test('hook: loading, erro público/fallback, retry, ordem, foco e preservação silenciosa', async () => {
  const h = harness();
  h.mount();
  assert.equal(h.state.carga.situacao, 'carregando');
  await act(async () => h.gets[0].reject(new Error('offline')));
  assert.equal(h.state.carga.mensagem, client.ERRO_CARREGAMENTO);
  act(() => h.state.recarregar());
  assert.equal(h.state.carga.situacao, 'carregando');
  await act(async () =>
    h.gets[1].reject(
      new client.ApiError(500, { code: 'ERRO', message: 'Indisponível.' })
    )
  );
  assert.equal(h.state.carga.mensagem, 'Indisponível.');
  act(() => h.state.recarregar());
  await h.resolveList(2, [pedido('z'), pedido('a')]);
  assert.deepEqual(
    h.state.carga.solicitacoes.map((p) => p.id),
    ['z', 'a']
  );
  act(() => h.router.focus());
  assert.equal(h.state.carga.situacao, 'sucesso');
  await act(async () => h.gets[3].reject(new Error('offline')));
  assert.equal(h.state.carga.solicitacoes.length, 2);
  assert.equal(h.state.carga.erroAtualizacao, client.ERRO_CARREGAMENTO);
  act(() => h.state.recarregar(true));
  await h.resolveList(4, []);
  assert.deepEqual(h.state.carga.solicitacoes, []);
  h.unmount();
});

for (const resposta of ['aceitar', 'recusar'])
  test(`${resposta}: só remove após confirmação, bloqueia duplicidade/oposição e reconsulta contador`, async () => {
    const h = harness();
    h.mount();
    await h.resolveList(0, [pedido(), pedido('p2')]);
    h.respond('p1', resposta);
    h.respond('p1', resposta === 'aceitar' ? 'recusar' : 'aceitar');
    h.respond('p1', resposta);
    assert.equal(h.posts.length, 1);
    assert.equal(h.state.acoes.p1, resposta);
    assert.equal(h.state.carga.solicitacoes.length, 2);
    await act(async () =>
      h.posts[0].resolve({
        id: 'p1',
        situacao: resposta === 'aceitar' ? 'aceita' : 'recusada',
      })
    );
    assert.deepEqual(
      h.state.carga.solicitacoes.map((p) => p.id),
      ['p2']
    );
    assert.deepEqual(h.state.acoes, {});
    assert.deepEqual(h.feedback, [
      resposta === 'aceitar' ? 'Pedido aceito.' : 'Pedido recusado.',
    ]);
    assert.equal(h.gets.length, 2);
    await h.resolveList(1, [pedido('novo'), pedido('p2')]);
    assert.equal(h.state.carga.solicitacoes.length, 2);
    h.unmount();
  });

test('falhas comuns e USUARIO_NAO_ENCONTRADO mantêm pedido e permitem nova resposta', async () => {
  const h = harness();
  h.mount();
  await h.resolveList(0, [pedido()]);
  for (const erro of [
    new Error('offline'),
    new client.ApiError(404, {
      code: 'USUARIO_NAO_ENCONTRADO',
      message: 'Usuário indisponível.',
    }),
    new client.ApiError(500, { message: 'diagnóstico privado' }),
  ]) {
    h.respond('p1', 'aceitar');
    await act(async () => h.posts.at(-1).reject(erro));
    assert.equal(h.state.carga.solicitacoes.length, 1);
    assert.deepEqual(h.state.acoes, {});
    assert.equal(h.gets.length, 1);
    assert.equal(
      h.feedback.at(-1),
      erro.publicMessage ?? 'Não foi possível concluir. Tente novamente.'
    );
  }
  h.respond('p1', 'recusar');
  assert.equal(h.posts.length, 4);
  h.unmount();
  assert.equal(h.posts[3].signal.aborted, true);
});

for (const [status, code] of [
  [409, 'SOLICITACAO_JA_RESPONDIDA'],
  [404, 'SOLICITACAO_NAO_ENCONTRADA'],
])
  test(`${status}/${code}: reconcilia lista/contador sem anunciar sucesso`, async () => {
    const h = harness();
    h.mount();
    await h.resolveList(0, [pedido(), pedido('p2')]);
    h.respond('p1', 'aceitar');
    await act(async () =>
      h.posts[0].reject(
        new client.ApiError(status, {
          code,
          message: 'Pedido não está mais disponível.',
        })
      )
    );
    assert.deepEqual(h.feedback, ['Pedido não está mais disponível.']);
    assert.equal(h.state.carga.solicitacoes.length, 2);
    assert.equal(h.gets.length, 2);
    await h.resolveList(1, [pedido('p2')]);
    assert.equal(h.state.carga.solicitacoes.length, 1);
    h.unmount();
  });

for (const origem of ['lista', 'resposta'])
  test(`403 na ${origem}: encerra acesso e aborta operações concorrentes`, async () => {
    const h = harness();
    h.mount();
    if (origem === 'resposta') {
      await h.resolveList(0, [pedido(), pedido('p2')]);
      h.respond('p1', 'aceitar');
      h.respond('p2', 'recusar');
    }
    await act(async () =>
      (origem === 'lista' ? h.gets[0] : h.posts[0]).reject(
        new client.ApiError(403, {
          code: 'SO_LIDER_CRIA_QUIZ',
          message: 'Só o administrador atual.',
        })
      )
    );
    assert.deepEqual(h.denials, ['Só o administrador atual.']);
    assert.equal(h.state.carga.situacao, 'erro');
    assert.deepEqual(h.feedback, []);
    if (origem === 'resposta') {
      assert.equal(h.posts[1].signal.aborted, true);
      await act(async () =>
        h.posts[1].resolve({ id: 'p2', situacao: 'recusada' })
      );
      assert.deepEqual(h.feedback, []);
    }
    h.respond('p1', 'aceitar');
    assert.equal(h.posts.length, origem === 'resposta' ? 2 : 0);
    h.unmount();
  });

for (const origem of ['lista', 'resposta'])
  test(`404 CLUBE_NAO_ENCONTRADO na ${origem}: recurso indisponível, retry e nenhum sucesso`, async () => {
    const h = harness();
    h.mount();
    if (origem === 'resposta') {
      await h.resolveList(0, [pedido()]);
      h.respond('p1', 'aceitar');
    }
    await act(async () =>
      (origem === 'lista' ? h.gets[0] : h.posts[0]).reject(
        new client.ApiError(404, {
          code: 'CLUBE_NAO_ENCONTRADO',
          message: 'Clube não encontrado.',
        })
      )
    );
    assert.deepEqual(h.feedback, []);
    assert.equal(h.state.carga.situacao, 'erro');
    assert.equal(h.state.carga.mensagem, 'Clube não encontrado.');
    act(() => h.state.recarregar());
    await h.resolveList(1, []);
    assert.equal(h.state.carga.situacao, 'sucesso');
    h.unmount();
  });

test('GET anterior à mutação não restaura pedido; respostas independentes concorrentes invalidam consultas anteriores', async () => {
  const h = harness();
  h.mount();
  await h.resolveList(0, [pedido(), pedido('p2')]);
  act(() => h.state.recarregar(true));
  h.respond('p1', 'aceitar');
  assert.equal(h.gets[1].signal.aborted, true);
  h.respond('p2', 'recusar');
  await act(async () => h.posts[0].resolve({ id: 'p1', situacao: 'aceita' }));
  assert.equal(h.gets.length, 3);
  await act(async () => h.posts[1].resolve({ id: 'p2', situacao: 'recusada' }));
  assert.equal(h.gets[2].signal.aborted, true);
  await h.resolveList(3, []);
  await h.resolveList(2, [pedido('p2')]);
  await h.resolveList(1, [pedido(), pedido('p2')]);
  assert.deepEqual(h.state.carga.solicitacoes, []);
  h.unmount();
});

test('troca de clube, blur e desmontagem cancelam consultas/respostas e impedem toast tardio', async () => {
  const h = harness();
  h.mount();
  await h.resolveList(0, [pedido()]);
  h.respond('p1', 'aceitar');
  act(() => h.state.recarregar(true));
  const frames = h.frames.length;
  h.change('b');
  assert.ok(
    h.frames.slice(frames).every((s) => s.carga.situacao === 'carregando')
  );
  assert.equal(h.posts[0].signal.aborted, true);
  assert.equal(h.gets[1].signal.aborted, true);
  await h.resolveList(2, [pedido('b')]);
  await act(async () => h.posts[0].resolve({ id: 'p1', situacao: 'aceita' }));
  await h.resolveList(1, [pedido()]);
  assert.deepEqual(h.feedback, []);
  assert.deepEqual(
    h.state.carga.solicitacoes.map((p) => p.id),
    ['b']
  );
  h.respond('b', 'recusar');
  act(() => h.router.blur());
  assert.equal(h.posts[1].signal.aborted, true);
  await act(async () =>
    h.posts[1].reject(
      new client.ApiError(403, {
        code: 'SO_LIDER_CRIA_QUIZ',
        message: 'Tardio',
      })
    )
  );
  assert.deepEqual(h.denials, []);
  act(() => h.router.focus());
  h.unmount();
  assert.equal(h.gets[3].signal.aborted, true);
  await h.resolveList(3, []);
  assert.deepEqual(h.feedback, []);
});

function screenDependencies(router, toasts, api) {
  const deps = ui(router, toasts);
  const { LinhaUsuario } = load('components/LinhaUsuario.tsx', {
    ...deps,
    './avatar': { Avatar: 'Avatar' },
  });
  const hooks = load('features/clube/useSolicitacoesClube.ts', {
    'expo-router': router,
    '@/api/client': client,
    './api': api,
  });
  return {
    ...deps,
    '@/components/LinhaUsuario': { LinhaUsuario },
    '@/features/conquistas/model': load('features/conquistas/model.ts'),
    './useSolicitacoesClube': hooks,
  };
}

test('tela real: estados, dados/data/avatar, @ único, ordem, perfil isolado, loading por ação, contador e retry', async () => {
  const h = harness(),
    toasts = [];
  const { SolicitacoesClubeScreen } = load(
    'features/clube/SolicitacoesClubeScreen.tsx',
    screenDependencies(h.router, toasts, h.api)
  );
  let tree;
  act(() => {
    tree = create(
      React.createElement(SolicitacoesClubeScreen, { clubeId: 'a' })
    );
  });
  assert.equal(
    tree.root.findByType('ActivityIndicator').props.accessibilityLabel,
    'Carregando solicitações'
  );
  assert.equal(tree.root.findAllByType('Card').length, 0);
  await act(async () => h.gets[0].reject(new Error('offline')));
  assert.equal(
    tree.root.findByProps({ accessibilityRole: 'alert' }).props.style.color,
    theme.colors.danger
  );
  act(() => {
    button(tree, 'Tentar de novo').props.onPress();
  });
  assert.equal(tree.root.findAllByType('Card').length, 0);
  await h.resolveList(1, []);
  assert.equal(
    tree.root.findByType('EmptyState').props.message,
    'Nenhum pedido para entrar no clube.'
  );
  assert.equal(tree.root.findByType('AppHeader').props.title, 'Solicitações');
  act(() => h.router.focus());
  await h.resolveList(2, [pedido('z', '@ana'), pedido('a', '@@bia')]);
  assert.deepEqual(
    tree.root.findAllByType('Avatar').map((n) => n.props.name),
    ['Nome z', 'Nome a']
  );
  assert.equal(tree.root.findAllByType('Avatar')[0].props.photoUrl, null);
  const profiles = tree.root.findAllByType('Pressable');
  assert.deepEqual(
    profiles.map((n) => n.props.accessibilityLabel),
    ['Nome z, @ana', 'Nome a, @bia']
  );
  assert.equal(profiles[0].findAllByType('PrimaryButton').length, 0);
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some((n) => n.props.children === 'Pedido em 02/10/2026')
  );
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some(
        (n) =>
          Array.isArray(n.props.children) &&
          n.props.children.join('') === '@ana'
      )
  );
  act(() => {
    profiles[0].props.onPress();
  });
  assert.deepEqual(h.router.navigations, [
    ['push', '/(tabs)/(feed)/feed-usuario/u-z'],
  ]);
  act(() => {
    button(tree, 'Aceitar').props.onPress();
    button(tree, 'Recusar').props.onPress();
    button(tree, 'Aceitar').props.onPress();
  });
  assert.equal(h.posts.length, 1);
  assert.equal(button(tree, 'Aceitar').props.loading, true);
  assert.equal(button(tree, 'Aceitar').props.disabled, true);
  assert.equal(button(tree, 'Recusar').props.disabled, true);
  assert.equal(button(tree, 'Recusar').props.loading, false);
  assert.equal(h.router.navigations.length, 1);
  await act(async () =>
    h.posts[0].reject(
      new client.ApiError(404, {
        code: 'USUARIO_NAO_ENCONTRADO',
        message: 'Usuário indisponível.',
      })
    )
  );
  assert.equal(tree.root.findAllByType('Card').length, 2);
  assert.equal(button(tree, 'Aceitar').props.disabled, false);
  assert.deepEqual(toasts, [['feedback', 'Usuário indisponível.']]);
  act(() => {
    button(tree, 'Recusar').props.onPress();
  });
  assert.equal(button(tree, 'Recusar').props.loading, true);
  assert.equal(button(tree, 'Aceitar').props.loading, false);
  await act(async () => h.posts[1].resolve({ id: 'z', situacao: 'recusada' }));
  assert.equal(tree.root.findAllByType('Card').length, 1);
  assert.equal(tree.root.findByType('AppHeader').props.title, 'Solicitações');
  assert.deepEqual(toasts.at(-1), ['feedback', 'Pedido recusado.']);
  await h.resolveList(3, [pedido('a', '@@bia')]);
  act(() => h.router.focus());
  await act(async () => h.gets[4].reject(new Error('offline')));
  assert.equal(tree.root.findAllByType('Card').length, 1);
  assert.ok(button(tree, 'Tentar de novo'));
  act(() => {
    button(tree, 'Tentar de novo').props.onPress();
  });
  assert.equal(tree.root.findAllByType('Card').length, 1);
  await h.resolveList(5, []);
  act(() => tree.unmount());
});

test('403 na tela substitui por detalhe para revalidar cargo e usa Toast após navegação', async () => {
  const h = harness(),
    toasts = [];
  const { SolicitacoesClubeScreen } = load(
    'features/clube/SolicitacoesClubeScreen.tsx',
    screenDependencies(h.router, toasts, h.api)
  );
  let tree;
  act(() => {
    tree = create(
      React.createElement(SolicitacoesClubeScreen, { clubeId: 'a' })
    );
  });
  await h.resolveList(0, [pedido()]);
  act(() => {
    button(tree, 'Aceitar').props.onPress();
  });
  await act(async () =>
    h.posts[0].reject(
      new client.ApiError(403, {
        code: 'SO_LIDER_CRIA_QUIZ',
        message: 'Cargo alterado.',
      })
    )
  );
  assert.deepEqual(h.router.navigations, [['replace', '/clube/a']]);
  assert.deepEqual(toasts, [['navigation', 'Cargo alterado.']]);
  assert.equal(tree.root.findAllByType('Card').length, 0);
  act(() => tree.unmount());
});

function paginaDeps(router, hooks) {
  return {
    ...ui(router),
    './useClube': hooks,
    './api': {},
    './RankingCard': { RankingCard: 'RankingCard' },
    './EntradaClubeButton': { EntradaClubeButton: 'EntradaClubeButton' },
    '@/features/livro/api': {},
    '@/features/quiz/api': { ERRO_QUIZ: {} },
    '@/features/quiz/retornoDoQuiz': {
      consumirRetorno: () => null,
      textoDoResumo: () => '',
    },
    '@/features/progresso/RegistrarProgressoSheet': {
      RegistrarProgressoSheet: 'RegistrarProgressoSheet',
    },
  };
}

for (const [cargo, count, situacao] of [
  ['administrador', 0, 'membro'],
  ['administrador', 4, 'membro'],
  ['administrador', undefined, 'membro'],
  ['membro', 9, 'membro'],
  [null, 9, 'nenhuma'],
  [null, 9, 'pendente'],
])
  test(`Página: acesso/contador ${cargo}/${count}/${situacao}`, () => {
    const router = focusRouter(),
      noop = () => {};
    const hooks = {
      useClube: () => ({
        clube: {
          situacao: 'sucesso',
          dados: {
            ...club(cargo, count),
            solicitacoesPendentes: count,
            situacao,
          },
        },
        recarregar: noop,
        atualizar: noop,
        pedidoEnviado: noop,
      }),
    };
    const { PaginaClubeScreen } = load(
      'features/clube/PaginaClubeScreen.tsx',
      paginaDeps(router, hooks)
    );
    let tree;
    act(() => {
      tree = create(React.createElement(PaginaClubeScreen, { clubeId: 'a' }));
    });
    const solicitacoes = buttons(tree).filter((n) =>
      n.props.label.startsWith('Solicitações')
    );
    assert.equal(solicitacoes.length, cargo === 'administrador' ? 1 : 0);
    if (cargo === 'administrador') {
      assert.equal(
        solicitacoes[0].props.label,
        count === undefined ? 'Solicitações' : `Solicitações (${count})`
      );
      act(() => {
        solicitacoes[0].props.onPress();
      });
      assert.deepEqual(router.navigations, [['push', '/clube/a/solicitacoes']]);
      assert.ok(button(tree, 'Sair do clube'));
      assert.ok(button(tree, 'Acessar Quiz'));
    }
    act(() => tree.unmount());
  });

test('rota administrativa real confirma cargo antes da lista, permite promoção e revoga após sucessão/foco', async () => {
  const router = focusRouter(),
    clubRequests = [],
    listRequests = [];
  router.useLocalSearchParams = () => ({ clubeId: 'a' });
  const { AcessoClube } = load('features/clube/AcessoClube.tsx', {
    ...ui(router),
    './api': {
      buscarClube: (id, signal) => {
        const d = deferred();
        clubRequests.push({ ...d, signal });
        return d.promise;
      },
    },
  });
  const noop = () => {};
  const { useSolicitacoesClube } = load(
    'features/clube/useSolicitacoesClube.ts',
    {
      'expo-router': router,
      '@/api/client': client,
      './api': {
        buscarSolicitacoes: (id, signal) => {
          const d = deferred();
          listRequests.push({ ...d, signal });
          return d.promise;
        },
      },
    }
  );
  function Admin({ clubeId }) {
    useSolicitacoesClube(clubeId, { onFeedback: noop, onSemAcesso: noop });
    return React.createElement('Admin');
  }
  const { default: Route } = load(
    'app/(tabs)/(feed)/clube/[clubeId]/solicitacoes.tsx',
    {
      'expo-router': router,
      '@/features/clube/AcessoClube': { AcessoClube },
      '@/features/clube/SolicitacoesClubeScreen': {
        SolicitacoesClubeScreen: Admin,
      },
    }
  );
  let tree;
  act(() => {
    tree = create(React.createElement(Route));
  });
  assert.equal(listRequests.length, 0);
  await act(async () => clubRequests[0].resolve(club('membro', 99)));
  assert.equal(listRequests.length, 0);
  assert.equal(visiveis(tree, 'Admin').length, 0);
  act(() => router.focus());
  await act(async () => clubRequests[1].resolve(club()));
  assert.equal(listRequests.length, 1);
  assert.equal(visiveis(tree, 'Admin').length, 1);
  act(() => router.blur());
  assert.equal(listRequests[0].signal.aborted, true);
  assert.equal(visiveis(tree, 'Admin').length, 0);
  act(() => router.focus());
  assert.equal(listRequests.length, 1);
  await act(async () => clubRequests[2].resolve(club('membro')));
  assert.equal(listRequests.length, 1);
  assert.equal(visiveis(tree, 'Admin').length, 0);
  act(() => tree.unmount());
});

test('rota de perfil na pilha Feed reutiliza PerfilScreen e mantém o ID', () => {
  const { default: Route } = load(
    'app/(tabs)/(feed)/feed-usuario/[usuarioId].tsx',
    {
      'expo-router': { useLocalSearchParams: () => ({ usuarioId: 'u-p1' }) },
      '@/features/perfil/PerfilScreen': { PerfilScreen: 'PerfilScreen' },
    }
  );
  let tree;
  act(() => {
    tree = create(React.createElement(Route));
  });
  assert.equal(tree.root.findByType('PerfilScreen').props.usuarioId, 'u-p1');
  assert.equal(tree.root.findByType('PerfilScreen').props.origem, 'feed');
  act(() => tree.unmount());
});

test('respostas confirmadas sincronizam contador/ranking/lista pública por nova consulta; recusa não inventa membro', async () => {
  let pendentes = [pedido(), pedido('p2')],
    total = 1;
  const adminRouter = focusRouter(),
    detailRouter = focusRouter(),
    publicRouter = focusRouter();
  const api = {
    buscarSolicitacoes: async () => ({ solicitacoes: [...pendentes] }),
    responderSolicitacao: async (c, id, acao) => {
      pendentes = pendentes.filter((p) => p.id !== id);
      if (acao === 'aceitar') total++;
      return { id, situacao: acao === 'aceitar' ? 'aceita' : 'recusada' };
    },
    buscarClube: async () => ({
      ...club('administrador', pendentes.length),
      ranking: [
        {
          posicao: 1,
          pontos: 0,
          usuario: { id: 'backend', nome: 'Confirmado', avatarUrl: null },
        },
      ],
    }),
    buscarClubes: async () => ({
      clubes: [{ id: 'a', membros: total, situacao: 'membro' }],
    }),
  };
  const detailHooks = load('features/clube/useClube.ts', {
    '@/api/client': client,
    './api': api,
  });
  const publicHooks = load('features/clube/useTodosClubes.ts', {
    'expo-router': publicRouter,
    '@/api/client': client,
    './api': api,
  });
  const adminHooks = load('features/clube/useSolicitacoesClube.ts', {
    'expo-router': adminRouter,
    '@/api/client': client,
    './api': api,
  });
  const { PaginaClubeScreen } = load(
    'features/clube/PaginaClubeScreen.tsx',
    paginaDeps(detailRouter, detailHooks)
  );
  let publicState, adminState;
  const noop = () => {};
  function Public() {
    publicState = publicHooks.useTodosClubes();
    return null;
  }
  function Admin() {
    adminState = adminHooks.useSolicitacoesClube('a', {
      onFeedback: noop,
      onSemAcesso: noop,
    });
    return null;
  }
  let dt, pt, at;
  await act(async () => {
    dt = create(React.createElement(PaginaClubeScreen, { clubeId: 'a' }));
    pt = create(React.createElement(Public));
    at = create(React.createElement(Admin));
  });
  assert.ok(button(dt, 'Solicitações (2)'));
  assert.equal(publicState.estado.clubes[0].membros, 1);
  await act(async () => {
    await adminState.responder('p1', 'aceitar');
  });
  assert.equal(adminState.carga.solicitacoes.length, 1);
  await act(async () => {
    detailRouter.focus();
    publicRouter.focus();
  });
  assert.ok(button(dt, 'Solicitações (1)'));
  assert.equal(publicState.estado.clubes[0].membros, 2);
  assert.equal(
    dt.root.findByType('RankingCard').props.ranking[0].usuario.id,
    'backend'
  );
  await act(async () => {
    await adminState.responder('p2', 'recusar');
  });
  await act(async () => {
    detailRouter.focus();
    publicRouter.focus();
  });
  assert.ok(button(dt, 'Solicitações (0)'));
  assert.equal(publicState.estado.clubes[0].membros, 2);
  // Troca externa de liderança é refletida na mesma Página ao ganhar foco.
  api.buscarClube = async () => club('membro', 0);
  await act(async () => detailRouter.focus());
  assert.equal(button(dt, 'Solicitações (0)'), undefined);
  assert.ok(button(dt, 'Sair do clube'));
  act(() => {
    dt.unmount();
    pt.unmount();
    at.unmount();
  });
});

for (const resposta of ['aceitar', 'recusar'])
  test(`servidor confirma ${resposta} após blur: retorno reconsulta sem presumir rollback do POST`, async () => {
    const h = harness();
    h.mount();
    await h.resolveList(0, [pedido()]);
    h.respond('p1', resposta);
    act(() => h.router.blur());
    assert.equal(h.posts[0].signal.aborted, true);
    await act(async () =>
      h.posts[0].resolve({
        id: 'p1',
        situacao: resposta === 'aceitar' ? 'aceita' : 'recusada',
      })
    );
    assert.deepEqual(h.feedback, []);
    assert.equal(h.state.carga.solicitacoes.length, 1);
    act(() => h.router.focus());
    assert.equal(h.gets.length, 2);
    await h.resolveList(1, []);
    assert.equal(h.state.carga.solicitacoes.length, 0);
    assert.deepEqual(h.state.acoes, {});
    h.unmount();
  });

test('cliente HTTP usa a sessão existente nos pedidos e respostas, sem inventar payload', async () => {
  const originalFetch = global.fetch,
    chamadas = [];
  client.configurarAutenticacao({
    obterToken: async () => 'token-de-teste',
    renovarToken: async () => null,
    aoPerderSessao: async () => {},
  });
  global.fetch = async (url, init) => {
    chamadas.push({ url, init });
    return new Response(
      JSON.stringify({
        id: 'pedido',
        situacao: url.endsWith('/aceitar') ? 'aceita' : 'pendente',
      }),
      { status: url.endsWith('/aceitar') ? 200 : 201 }
    );
  };
  try {
    const api = load('features/clube/api.ts', { '@/api/client': client });
    assert.deepEqual(await api.solicitarEntrada('a'), {
      id: 'pedido',
      situacao: 'pendente',
    });
    assert.deepEqual(await api.solicitarEntrada('a'), {
      id: 'pedido',
      situacao: 'pendente',
    });
    assert.deepEqual(await api.responderSolicitacao('a', 'pedido', 'aceitar'), {
      id: 'pedido',
      situacao: 'aceita',
    });
    assert.equal(chamadas.length, 3);
    for (const { init } of chamadas) {
      assert.equal(init.method, 'POST');
      assert.equal(init.headers.get('Authorization'), 'Bearer token-de-teste');
      assert.equal(init.body, undefined);
    }
  } finally {
    global.fetch = originalFetch;
    client.configurarAutenticacao(null);
  }
});

test('perfil no Feed volta à pilha atual e usa /feed se não houver histórico, sem mudar fallback da Busca', () => {
  const router = focusRouter(),
    navigation = router.useRouter(),
    noop = () => {};
  navigation.canGoBack = () => false;
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...ui(router),
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/BookCover': { BookCover: 'BookCover' },
    '@/components/icons/PawIcon': { PawIcon: 'PawIcon' },
    '@/features/conquistas/IconeConquista': {
      IconeConquista: 'IconeConquista',
    },
    './NivelAnel': { NivelAnel: 'NivelAnel' },
    './PerfilTopo': { PerfilTopo: 'PerfilTopo' },
    './usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'carregando' },
        recarregar: noop,
        atualizar: noop,
        alternarSeguir: noop,
        alterandoSeguir: false,
        proprio: false,
      }),
    },
  });
  let tree;
  act(() => {
    tree = create(
      React.createElement(PerfilScreen, { usuarioId: 'u-p1', origem: 'feed' })
    );
  });
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.deepEqual(router.navigations.at(-1), ['replace', '/feed']);
  navigation.canGoBack = () => true;
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.deepEqual(router.navigations.at(-1), ['back']);
  navigation.canGoBack = () => false;
  act(() =>
    tree.update(React.createElement(PerfilScreen, { usuarioId: 'u-p1' }))
  );
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.deepEqual(router.navigations.at(-1), ['replace', '/buscar']);
  act(() => tree.unmount());
});
