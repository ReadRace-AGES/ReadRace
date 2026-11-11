const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;
function load(file, deps = {}) {
  if (file === 'features/feed/useFeedComunidades.ts')
    deps = { '@/features/comunidade/api': {}, ...deps };
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
function deferred() {
  let resolve, reject;
  const promise = new Promise((a, b) => {
    resolve = a;
    reject = b;
  });
  return { promise, resolve, reject };
}
function focusRouter() {
  const callbacks = new Set();
  const cleanups = new Map();
  const navigations = [];
  return {
    useFocusEffect: (callback) =>
      React.useEffect(() => {
        callbacks.add(callback);
        cleanups.set(callback, callback());
        return () => {
          callbacks.delete(callback);
          cleanups.get(callback)?.();
          cleanups.delete(callback);
        };
      }, [callback]),
    useRouter: () => ({
      push: (route) => navigations.push(route),
      replace: (route) => navigations.push(route),
      back: () => navigations.push('back'),
      canGoBack: () => true,
    }),
    focus: () => {
      for (const cb of callbacks) {
        cleanups.get(cb)?.();
        cleanups.set(cb, cb());
      }
    },
    blur: () => {
      for (const cleanup of cleanups.values()) cleanup?.();
      cleanups.clear();
    },
    navigations,
  };
}
const native = {
  View: 'View',
  Text: 'Text',
  ScrollView: 'ScrollView',
  Pressable: 'Pressable',
  ActivityIndicator: 'ActivityIndicator',
  Keyboard: { dismiss() {} },
  StyleSheet: { create: (s) => s },
};
function ui(router, toasts = []) {
  return {
    'react-native': native,
    'expo-router': router,
    '@/api/client': client,
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    'react-native-svg': {
      __esModule: true,
      default: 'Svg',
      Path: 'Path',
      Rect: 'Rect',
      Circle: 'Circle',
    },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/EmptyState': {
      EmptyState: ({ action, ...props }) =>
        React.createElement('EmptyState', props, action),
    },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/BottomSheet': {
      BottomSheet: ({ children, ...props }) =>
        React.createElement('BottomSheet', props, children),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showToast: () => {},
        showErrorToast: (message) => toasts.push(message),
        showToastAfterNavigation: (message) => toasts.push(message),
      }),
    },
  };
}
const club = (
  situacao = 'nenhuma',
  id = 'a',
  cargo = situacao === 'membro' ? 'membro' : null
) => ({
  id,
  situacao,
  nome: 'Clube',
  meuCargo: cargo,
  livroAtual: { id: 'livro', titulo: 'Livro', autor: null },
  ranking: [],
  proximoLider: null,
  temQuizAtivo: false,
});
const listed = (situacao, id = 'a') => ({
  id,
  situacao,
  nome: id,
  capaUrl: null,
  livro: { titulo: 'Livro', autor: null },
  membros: 1,
});
const button = (tree, label) =>
  tree.root.findAllByType('PrimaryButton').find((n) => n.props.label === label);

test('contratos do cliente: lista, POST sem associação e resposta pendente repetida', async () => {
  const calls = [];
  const api = load('features/clube/api.ts', {
    '@/api/client': {
      apiGet: async (...args) => {
        calls.push(args);
        return { clubes: [listed('membro')] };
      },
      apiRequest: async (...args) => {
        calls.push(args);
        return { id: 'pedido', situacao: 'pendente' };
      },
    },
  });
  const signal = new AbortController().signal;
  assert.equal((await api.buscarClubes(signal)).clubes[0].situacao, 'membro');
  assert.deepEqual(
    await api.solicitarEntrada('a/b', signal),
    await api.solicitarEntrada('a/b', signal)
  );
  assert.deepEqual(calls, [
    ['/api/clubes', { signal }],
    ...Array(2).fill([
      '/api/clubes/a%2Fb/solicitacoes',
      { method: 'POST', signal },
    ]),
  ]);
});

test('listagem: loading, erro público/fallback, retry, ordem, foco, recusa/aceite, atualização silenciosa e cancelamento', async () => {
  const router = focusRouter(),
    requests = [];
  const { useTodosClubes } = load('features/clube/useTodosClubes.ts', {
    'expo-router': router,
    '@/api/client': client,
    './api': {
      buscarClubes: (signal) => {
        const d = deferred();
        requests.push({ ...d, signal });
        return d.promise;
      },
    },
  });
  let state;
  function Probe() {
    state = useTodosClubes();
    return null;
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  assert.equal(state.estado.situacao, 'carregando');
  await act(async () => requests[0].reject(new Error('offline')));
  assert.equal(state.estado.mensagem, client.ERRO_CARREGAMENTO);
  act(() => state.recarregar());
  assert.equal(state.estado.situacao, 'carregando');
  await act(async () =>
    requests[1].reject(
      new client.ApiError(503, { code: 'ERRO', message: 'Indisponível' })
    )
  );
  assert.equal(state.estado.mensagem, 'Indisponível');
  act(() => state.recarregar());
  const clubes = [
    listed('nenhuma', 'z'),
    listed('pendente', 'a'),
    listed('membro', 'm'),
  ];
  await act(async () => requests[2].resolve({ clubes }));
  assert.deepEqual(state.estado.clubes, clubes);
  act(() => router.focus());
  assert.deepEqual(state.estado.clubes, clubes);
  act(() => state.pedidoEnviado('z'));
  assert.equal(requests[3].signal.aborted, true);
  await act(async () => requests[3].resolve({ clubes }));
  assert.equal(state.estado.clubes[0].situacao, 'pendente');
  act(() => router.focus());
  await act(async () =>
    requests[4].resolve({
      clubes: [listed('nenhuma', 'z'), listed('membro', 'a')],
    })
  );
  assert.deepEqual(
    state.estado.clubes.map((c) => c.situacao),
    ['nenhuma', 'membro']
  );
  act(() => router.focus());
  await act(async () => requests[5].reject(new Error('offline')));
  assert.equal(state.estado.situacao, 'sucesso');
  act(() => state.recarregar());
  act(() => state.recarregar());
  assert.equal(requests[6].signal.aborted, true);
  await act(async () => requests[7].resolve({ clubes: [] }));
  await act(async () => requests[6].resolve({ clubes }));
  assert.deepEqual(state.estado.clubes, []);
  act(() => state.recarregar());
  act(() => tree.unmount());
  assert.equal(requests[8].signal.aborted, true);
  await act(async () => requests[8].resolve({ clubes }));
});

test('pedido: três rótulos, duplicidade, erro recuperável, mensagem pública, sucesso, JA_E_MEMBRO e abort', async () => {
  const requests = [],
    toasts = [],
    router = focusRouter();
  let pedidos = 0,
    reconciliacoes = 0;
  const { EntradaClubeButton } = load('features/clube/EntradaClubeButton.tsx', {
    ...ui(router, toasts),
    './api': {
      solicitarEntrada: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, id, signal });
        return d.promise;
      },
    },
  });
  const render = (situacao) =>
    React.createElement(EntradaClubeButton, {
      clubeId: 'a',
      situacao,
      onPedido: () => pedidos++,
      onReconciliar: () => reconciliacoes++,
    });
  let tree;
  act(() => {
    tree = create(render('membro'));
  });
  assert.equal(button(tree, 'Participando').props.disabled, true);
  act(() => tree.update(render('pendente')));
  assert.equal(button(tree, 'Pedido enviado').props.disabled, true);
  act(() => tree.update(render('nenhuma')));
  const send = button(tree, 'Pedir para entrar').props.onPress;
  act(() => {
    send();
    send();
  });
  assert.equal(requests.length, 1);
  assert.equal(button(tree, 'Pedir para entrar').props.loading, true);
  await act(async () => requests[0].reject(new client.ApiError(500, {})));
  assert.equal(pedidos, 0);
  assert.equal(button(tree, 'Pedir para entrar').props.disabled, false);
  assert.equal(toasts.at(-1), 'Não foi possível concluir. Tente novamente.');
  act(() => {
    button(tree, 'Pedir para entrar').props.onPress();
  });
  await act(async () =>
    requests[1].reject(
      new client.ApiError(409, {
        code: 'JA_E_MEMBRO',
        message: 'Você já participa.',
      })
    )
  );
  assert.equal(reconciliacoes, 1);
  assert.equal(pedidos, 0);
  assert.equal(toasts.at(-1), 'Você já participa.');
  act(() => {
    button(tree, 'Pedir para entrar').props.onPress();
  });
  await act(async () =>
    requests[2].resolve({ id: 'pedido', situacao: 'pendente' })
  );
  assert.equal(pedidos, 1);
  assert.equal(toasts.at(-1), 'Pedido enviado');
  act(() => tree.update(render('pendente')));
  assert.equal(button(tree, 'Pedido enviado').props.disabled, true);
  act(() => tree.update(render('nenhuma')));
  act(() => {
    button(tree, 'Pedir para entrar').props.onPress();
  });
  act(() => tree.unmount());
  assert.equal(requests[3].signal.aborted, true);
  await act(async () =>
    requests[3].resolve({ id: 'pedido', situacao: 'pendente' })
  );
  assert.equal(pedidos, 1);
});

test('Todos os clubes: estados, cards, navegação independente do botão e Criar Grupo reutilizado', async () => {
  const router = focusRouter();
  let estado = { situacao: 'carregando' },
    retries = 0,
    pedidos = [];
  const { TodosClubesScreen } = load('features/clube/TodosClubesScreen.tsx', {
    ...ui(router),
    './useTodosClubes': {
      useTodosClubes: () => ({
        estado,
        recarregar: () => retries++,
        pedidoEnviado: (id) => pedidos.push(id),
      }),
    },
    './EntradaClubeButton': { EntradaClubeButton: 'EntradaClubeButton' },
    '@/features/feed/ClubeCard': { ClubeCard: 'ClubeCard' },
    '@/features/feed/CriarGrupoSheet': { CriarGrupoSheet: 'CriarGrupoSheet' },
  });
  let tree;
  const render = () => React.createElement(TodosClubesScreen);
  act(() => {
    tree = create(render());
  });
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
  assert.equal(tree.root.findAllByType('ClubeCard').length, 0);
  estado = { situacao: 'erro', mensagem: 'Erro' };
  act(() => tree.update(render()));
  act(() => {
    button(tree, 'Tentar de novo').props.onPress();
  });
  assert.equal(retries, 1);
  estado = { situacao: 'sucesso', clubes: [] };
  act(() => tree.update(render()));
  assert.equal(
    tree.root.findByType('EmptyState').props.message,
    'Nenhum clube por aqui ainda.'
  );
  act(() => {
    button(tree, 'Criar Grupo').props.onPress();
  });
  assert.equal(tree.root.findByType('CriarGrupoSheet').props.visible, true);
  act(() => tree.root.findByType('CriarGrupoSheet').props.onSuccess());
  assert.equal(retries, 2);
  act(() => tree.root.findByType('CriarGrupoSheet').props.onClose());
  assert.equal(tree.root.findByType('CriarGrupoSheet').props.visible, false);
  estado = {
    situacao: 'sucesso',
    clubes: [
      listed('nenhuma', 'z'),
      listed('pendente', 'a'),
      listed('membro', 'b'),
    ],
  };
  act(() => tree.update(render()));
  assert.deepEqual(
    tree.root.findAllByType('ClubeCard').map((n) => n.props.clube.id),
    ['z', 'a', 'b']
  );
  act(() => tree.root.findAllByType('EntradaClubeButton')[0].props.onPedido());
  assert.deepEqual(pedidos, ['z']);
  assert.deepEqual(router.navigations, []);
  act(() => tree.root.findAllByType('ClubeCard')[0].props.onPress());
  assert.deepEqual(router.navigations, ['/clube/z']);
  assert.equal(
    tree.root.findAllByType('ClubeCard')[0].props.clube.totalMembros,
    1
  );
  act(() => tree.unmount());
});

test('card existente mantém imagem/fallback, livro e plural correto', () => {
  const { ClubeCard } = load('features/feed/ClubeCard.tsx', {
    ...ui(focusRouter()),
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/Card': {
      Card: ({ children, ...props }) =>
        React.createElement('Card', props, children),
    },
  });
  let tree;
  const render = (n) =>
    React.createElement(ClubeCard, {
      clube: {
        id: 'a',
        nome: 'Clube',
        capaUrl: null,
        livroAtual: { titulo: 'Livro', autor: 'Autora' },
        totalMembros: n,
      },
      onPress: () => {},
    });
  act(() => {
    tree = create(render(1));
  });
  assert.equal(
    tree.root.findByType('Card').props.accessibilityLabel,
    'Clube, Livro - Autora, 1 membro'
  );
  assert.equal(tree.root.findByType('Avatar').props.photoUrl, null);
  act(() => tree.update(render(0)));
  assert.match(
    tree.root.findByType('Card').props.accessibilityLabel,
    /0 membros/
  );
  act(() => tree.update(render(2)));
  assert.match(
    tree.root.findByType('Card').props.accessibilityLabel,
    /2 membros/
  );
  act(() => tree.unmount());
});

test('detalhe: preserva dados em atualização, descarta resposta antiga e dados de outro clube', async () => {
  const requests = [];
  const { useClube } = load('features/clube/useClube.ts', {
    '@/api/client': client,
    './api': {
      buscarClube: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, id, signal });
        return d.promise;
      },
    },
  });
  let state;
  const frames = [];
  function Probe({ id }) {
    state = useClube(id);
    frames.push(state.clube);
    return null;
  }
  let tree;
  act(() => {
    tree = create(React.createElement(Probe, { id: 'a' }));
  });
  await act(async () => requests[0].resolve(club()));
  act(() => state.atualizar());
  assert.equal(state.clube.situacao, 'sucesso');
  act(() => state.pedidoEnviado());
  await act(async () => requests[1].resolve(club()));
  assert.equal(state.clube.dados.situacao, 'pendente');
  act(() => state.atualizar());
  await act(async () => requests[2].resolve(club('membro')));
  assert.equal(state.clube.dados.meuCargo, 'membro');
  act(() => state.atualizar());
  await act(async () => requests[3].reject(new Error('offline')));
  assert.equal(state.clube.dados.situacao, 'membro');
  act(() => state.recarregar());
  act(() => state.recarregar());
  await act(async () => requests[5].resolve(club('pendente')));
  await act(async () => requests[4].resolve(club('membro')));
  assert.equal(state.clube.dados.situacao, 'pendente');
  frames.length = 0;
  act(() => tree.update(React.createElement(Probe, { id: 'b' })));
  assert.ok(frames.every((s) => s.situacao === 'carregando'));
  await act(async () => requests[6].resolve(club('nenhuma', 'b')));
  assert.equal(state.clube.dados.id, 'b');
  act(() => state.atualizar());
  act(() => tree.unmount());
  assert.equal(requests[7].signal.aborted, true);
});

for (const situacao of ['nenhuma', 'pendente', 'membro'])
  test(`rotas exclusivas: confirma ${situacao} antes de montar, revalida por foco e bloqueia liderança indevida`, async () => {
    const router = focusRouter(),
      requests = [];
    let montagens = 0;
    const { AcessoClube } = load('features/clube/AcessoClube.tsx', {
      ...ui(router),
      './api': {
        buscarClube: (id, signal) => {
          const d = deferred();
          requests.push({ ...d, id, signal });
          return d.promise;
        },
      },
    });
    function Content() {
      React.useEffect(() => {
        montagens++;
      }, []);
      return React.createElement('Conteudo');
    }
    const render = (administrador = false, id = 'a') =>
      React.createElement(
        AcessoClube,
        { clubeId: id, administrador },
        React.createElement(Content)
      );
    let tree;
    act(() => {
      tree = create(render());
    });
    assert.equal(montagens, 0);
    await act(async () => requests[0].resolve(club(situacao)));
    assert.equal(montagens, situacao === 'membro' ? 1 : 0);
    act(() => router.focus());
    assert.equal(visiveis(tree, 'Conteudo').length, 0);
    await act(async () => requests[1].resolve(club('pendente')));
    assert.equal(visiveis(tree, 'Conteudo').length, 0);
    act(() => tree.update(render(true)));
    await act(async () => requests[2].resolve(club('membro')));
    assert.equal(visiveis(tree, 'Conteudo').length, 0);
    act(() => router.focus());
    await act(async () =>
      requests[3].resolve(club('membro', 'a', 'administrador'))
    );
    assert.equal(visiveis(tree, 'Conteudo').length, 1);
    act(() => tree.update(render(true, 'b')));
    assert.equal(visiveis(tree, 'Conteudo').length, 0);
    act(() => router.focus());
    assert.equal(requests[4].signal.aborted, true);
    await act(async () =>
      requests[4].resolve(club('membro', 'b', 'administrador'))
    );
    assert.equal(visiveis(tree, 'Conteudo').length, 0);
    await act(async () => requests[5].reject(new Error('offline')));
    assert.equal(
      tree.root.findByType('EmptyState').props.message,
      client.ERRO_CARREGAMENTO
    );
    act(() => {
      button(tree, 'Tentar de novo').props.onPress();
    });
    act(() => tree.unmount());
    assert.equal(requests[6].signal.aborted, true);
  });

for (const [situacao, cargo] of [
  ['nenhuma', null],
  ['pendente', null],
  ['membro', 'membro'],
  ['membro', 'administrador'],
])
  test(`Página do clube: ${situacao}/${cargo}, handlers, quiz, leitura, saída e sucessão`, async () => {
    const router = focusRouter(),
      requests = [],
      toasts = [],
      livros = [],
      quizzes = [],
      saidas = [];
    const api = {
      buscarClube: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, id, signal });
        return d.promise;
      },
      sairDoClube: async (id) => {
        saidas.push(id);
        return { novoLider: { id: 'lider', nome: 'Ana' } };
      },
    };
    const hooks = load('features/clube/useClube.ts', {
      '@/api/client': client,
      './api': api,
    });
    const { PaginaClubeScreen } = load('features/clube/PaginaClubeScreen.tsx', {
      ...ui(router, toasts),
      './api': api,
      './useClube': hooks,
      './RankingCard': { RankingCard: 'RankingCard' },
      './EntradaClubeButton': { EntradaClubeButton: 'EntradaClubeButton' },
      '@/features/livro/api': {
        buscarDetalhe: async (id) => {
          livros.push(id);
          return { livro: { id, totalPaginas: 100 }, progresso: null };
        },
      },
      '@/features/quiz/api': {
        buscarQuiz: async (id) => {
          quizzes.push(id);
          return { proximaPergunta: { id: 'q' } };
        },
        ERRO_QUIZ: {},
      },
      '@/features/quiz/retornoDoQuiz': {
        consumirRetorno: () => null,
        textoDoResumo: () => '',
      },
      '@/features/progresso/RegistrarProgressoSheet': {
        RegistrarProgressoSheet: 'RegistrarProgressoSheet',
      },
    });
    let tree;
    act(() => {
      tree = create(React.createElement(PaginaClubeScreen, { clubeId: 'a' }));
    });
    assert.equal(button(tree, 'Registrar leitura').props.disabled, true);
    const dados = {
      ...club(situacao, 'a', cargo),
      proximoLider:
        cargo === 'administrador'
          ? { id: 'lider', nome: 'Ana', avatarUrl: null }
          : null,
      solicitacoesPendentes: 2,
    };
    await act(async () => requests[0].resolve(dados));
    const membro = situacao === 'membro';
    for (const label of ['Registrar leitura', 'Acessar Quiz', 'Chat do clube'])
      assert.equal(button(tree, label).props.disabled, !membro);
    assert.equal(button(tree, 'Fórum de leitura').props.disabled, false);
    await act(async () => {
      await button(tree, 'Registrar leitura').props.onPress();
      await button(tree, 'Acessar Quiz').props.onPress();
      button(tree, 'Fórum de leitura').props.onPress();
      button(tree, 'Chat do clube').props.onPress();
    });
    assert.equal(livros.length, membro ? 1 : 0);
    assert.equal(quizzes.length, cargo === 'membro' ? 1 : 0);
    if (!membro) {
      assert.deepEqual(router.navigations, ['/clube/a/forum']);
      assert.equal(button(tree, 'Sair do clube'), undefined);
      assert.equal(
        tree.root.findByType('EntradaClubeButton').props.situacao,
        situacao
      );
      assert.equal(
        tree.root.findByType('RankingCard').props.onVerTodosPress,
        undefined
      );
    } else {
      assert.equal(
        tree.root.findAllByType('RegistrarProgressoSheet').length,
        1
      );
      assert.ok(
        router.navigations.includes(
          cargo === 'administrador'
            ? '/clube/a/quiz/configurar'
            : '/clube/a/quiz'
        )
      );
      assert.ok(router.navigations.includes('/clube/a/forum'));
      assert.ok(router.navigations.some((r) => r.startsWith('/clube/a/chat')));
      act(() => tree.root.findByType('RankingCard').props.onVerTodosPress());
      assert.equal(router.navigations.at(-1), '/clube/a/ranking');
      act(() => button(tree, 'Sair do clube').props.onPress());
      const sheet = tree.root.findByType('BottomSheet');
      assert.equal(sheet.props.visible, true);
      if (cargo === 'administrador')
        assert.ok(
          tree.root
            .findAllByType('Text')
            .some(
              (n) =>
                n.props.children ===
                'Você é o único líder. Ao sair, Ana passa a liderar o clube.'
            )
        );
      await act(async () => {
        await button(tree, 'Sair').props.onPress();
      });
      assert.deepEqual(saidas, ['a']);
      assert.equal(router.navigations.at(-1), '/feed');
      assert.equal(toasts.at(-1), 'Você saiu do clube. Ana agora é o líder.');
    }
    // Retorno por foco consulta a associação atual e bloqueia recursos após recusa/saída externa.
    act(() => router.focus());
    await act(async () => requests[1].resolve(club('pendente')));
    assert.equal(button(tree, 'Registrar leitura').props.disabled, true);
    assert.equal(tree.root.findAllByType('RegistrarProgressoSheet').length, 0);
    assert.equal(
      tree.root.findByType('EntradaClubeButton').props.situacao,
      'pendente'
    );
    act(() => tree.unmount());
  });

test('lista e detalhe convergem por foco; recusa permite novo pedido, JA_E_MEMBRO consulta dados reais e Meus Grupos atualiza', async () => {
  let situacao = 'nenhuma';
  const listRouter = focusRouter(),
    detailRouter = focusRouter();
  const toasts = [];
  let error = null;
  const api = {
    buscarClube: async () => club(situacao),
    buscarClubes: async () => ({ clubes: [listed(situacao)] }),
    solicitarEntrada: async () => {
      if (error) throw error;
      situacao = 'pendente';
      return { id: 'mesmo-pedido', situacao: 'pendente' };
    },
  };
  const listHooks = load('features/clube/useTodosClubes.ts', {
    'expo-router': listRouter,
    '@/api/client': client,
    './api': api,
  });
  const detailHooks = load('features/clube/useClube.ts', {
    '@/api/client': client,
    './api': api,
  });
  const { EntradaClubeButton } = load('features/clube/EntradaClubeButton.tsx', {
    ...ui(detailRouter, toasts),
    './api': api,
  });
  let list, detail;
  function List() {
    list = listHooks.useTodosClubes();
    const c = list.estado.situacao === 'sucesso' ? list.estado.clubes[0] : null;
    return c
      ? React.createElement(EntradaClubeButton, {
          clubeId: c.id,
          situacao: c.situacao,
          onPedido: () => list.pedidoEnviado(c.id),
          onReconciliar: () => list.recarregar(true),
        })
      : null;
  }
  function Detail() {
    detail = detailHooks.useClube('a');
    const c = detail.clube.situacao === 'sucesso' ? detail.clube.dados : null;
    return c
      ? React.createElement(EntradaClubeButton, {
          clubeId: c.id,
          situacao: c.situacao,
          onPedido: detail.pedidoEnviado,
          onReconciliar: detail.atualizar,
        })
      : null;
  }
  let lt, dt;
  await act(async () => {
    lt = create(React.createElement(List));
    dt = create(React.createElement(Detail));
  });
  await act(async () => {
    await button(lt, 'Pedir para entrar').props.onPress();
  });
  assert.equal(button(lt, 'Pedido enviado').props.disabled, true);
  await act(async () => detail.atualizar());
  assert.equal(button(dt, 'Pedido enviado').props.disabled, true);
  situacao = 'nenhuma';
  await act(async () => detail.atualizar());
  assert.equal(button(dt, 'Pedir para entrar').props.disabled, false);
  await act(async () => {
    await button(dt, 'Pedir para entrar').props.onPress();
  });
  assert.equal(button(dt, 'Pedido enviado').props.disabled, true);
  await act(async () => listRouter.focus());
  assert.equal(button(lt, 'Pedido enviado').props.disabled, true);
  situacao = 'nenhuma';
  await act(async () => detail.atualizar());
  // Associação confirmada fora do app entre o GET e o POST.
  situacao = 'membro';
  error = new client.ApiError(409, {
    code: 'JA_E_MEMBRO',
    message: 'Já é membro.',
  });
  await act(async () => {
    await button(dt, 'Pedir para entrar').props.onPress();
  });
  assert.equal(detail.clube.dados.meuCargo, 'membro');
  assert.equal(button(dt, 'Participando').props.disabled, true);
  await act(async () => listRouter.focus());
  assert.equal(button(lt, 'Participando').props.disabled, true);
  const { useFeedComunidades } = load('features/feed/useFeedComunidades.ts', {
    '@/api/client': client,
    './api': {
      buscarFeedComunidades: async () => ({
        clubes: situacao === 'membro' ? [{ id: 'a' }] : [],
        comunidades: [],
        usuario: {},
      }),
    },
  });
  let feed;
  function Feed() {
    feed = useFeedComunidades();
    return null;
  }
  let ft;
  await act(async () => {
    ft = create(React.createElement(Feed));
  });
  assert.deepEqual(feed.feed.dados.clubes, [{ id: 'a' }]);
  situacao = 'nenhuma';
  await act(async () => feed.atualizar());
  assert.deepEqual(feed.feed.dados.clubes, []);
  act(() => {
    lt.unmount();
    dt.unmount();
    ft.unmount();
  });
});

test('rotas existentes usam proteção de clube com o cargo correspondente; Ver todos abre a nova rota', () => {
  const router = focusRouter();
  router.useLocalSearchParams = () => ({
    clubeId: 'a',
    titulo: 'Quiz',
    recompensaXp: '100',
    temQuizAtivo: 'false',
  });
  const routes = [
    ['chat.tsx', '@/features/chat/ChatClubeScreen', 'ChatClubeScreen'],
    [
      'ranking.tsx',
      '@/features/clube/RankingClubeScreen',
      'RankingClubeScreen',
    ],
    ['quiz.tsx', '@/features/quiz/QuizScreen', 'QuizScreen'],
    [
      'quiz/configurar.tsx',
      '@/features/clube/quiz/ConfigurarQuizScreen',
      'ConfigurarQuizScreen',
    ],
    [
      'quiz/adicionar-perguntas.tsx',
      '@/features/clube/quiz/AdicionarPerguntasScreen',
      'AdicionarPerguntasScreen',
    ],
  ];
  for (const [file, dep, component] of routes) {
    const { default: Route } = load(
      `app/(tabs)/(feed)/clube/[clubeId]/${file}`,
      {
        'expo-router': router,
        '@/features/clube/AcessoClube': { AcessoClube: 'AcessoClube' },
        [dep]: { [component]: component },
      }
    );
    let tree;
    act(() => {
      tree = create(React.createElement(Route));
    });
    assert.equal(tree.root.findByType('AcessoClube').props.clubeId, 'a');
    assert.equal(
      Boolean(tree.root.findByType('AcessoClube').props.administrador),
      file.startsWith('quiz/')
    );
    act(() => tree.unmount());
  }
  router.router = router.useRouter();
  const { default: Feed } = load('app/(tabs)/(feed)/feed.tsx', {
    ...ui(router),
    '@/features/feed/useFeedComunidades': {
      useFeedComunidades: () => ({
        feed: { situacao: 'sucesso', dados: {} },
        recarregar: () => {},
        atualizar: () => {},
      }),
    },
    '@/features/feed/FeedComunidadesView': {
      FeedComunidadesView: 'FeedComunidadesView',
    },
    '@/features/feed/useFeedSocial': {
      useFeedSocial: () => ({ atualizar: () => {} }),
    },
    '@/features/feed/CriarGrupoSheet': { CriarGrupoSheet: 'CriarGrupoSheet' },
  });
  let tree;
  act(() => {
    tree = create(React.createElement(Feed));
  });
  act(() =>
    tree.root.findByType('FeedComunidadesView').props.onVerTodosPress()
  );
  assert.equal(router.navigations.at(-1), '/clubes');
  act(() =>
    tree.root.findByType('FeedComunidadesView').props.onCriarGrupoPress()
  );
  assert.equal(tree.root.findByType('CriarGrupoSheet').props.visible, true);
  act(() => tree.unmount());
});

test('sair de uma rota exclusiva oculta conteúdo e suspende seus efeitos antes do próximo foco', async () => {
  const router = focusRouter(),
    requests = [];
  let leiturasPrivadas = 0;
  const { AcessoClube } = load('features/clube/AcessoClube.tsx', {
    ...ui(router),
    './api': {
      buscarClube: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, signal });
        return d.promise;
      },
    },
  });
  function Conteudo() {
    React.useEffect(() => {
      leiturasPrivadas++;
    }, []);
    return React.createElement('Conteudo');
  }
  let tree;
  act(() => {
    tree = create(
      React.createElement(
        AcessoClube,
        { clubeId: 'a' },
        React.createElement(Conteudo)
      )
    );
  });
  await act(async () => requests[0].resolve(club('membro')));
  assert.equal(leiturasPrivadas, 1);
  act(() => router.blur());
  assert.equal(visiveis(tree, 'Conteudo').length, 0);
  act(() => router.focus());
  assert.equal(leiturasPrivadas, 1);
  act(() => router.blur());
  assert.equal(requests[1].signal.aborted, true);
  await act(async () => requests[1].resolve(club('membro')));
  assert.equal(visiveis(tree, 'Conteudo').length, 0);
  act(() => router.focus());
  await act(async () => requests[2].resolve(club('nenhuma')));
  assert.equal(leiturasPrivadas, 1);
  act(() => tree.unmount());
});

test('pedido confirmado no servidor após blur é reconciliado no foco sem Toast ou atualização tardia', async () => {
  const router = focusRouter(),
    toasts = [],
    posts = [];
  let situacao = 'nenhuma',
    lista;
  const api = {
    buscarClubes: async () => ({ clubes: [listed(situacao)] }),
    solicitarEntrada: (id, signal) => {
      const d = deferred();
      posts.push({ ...d, signal });
      return d.promise;
    },
  };
  const hooks = load('features/clube/useTodosClubes.ts', {
    'expo-router': router,
    '@/api/client': client,
    './api': api,
  });
  const { EntradaClubeButton } = load('features/clube/EntradaClubeButton.tsx', {
    ...ui(router, toasts),
    './api': api,
  });
  function Tela() {
    lista = hooks.useTodosClubes();
    const clube =
      lista.estado.situacao === 'sucesso' ? lista.estado.clubes[0] : null;
    return (
      clube &&
      React.createElement(EntradaClubeButton, {
        clubeId: clube.id,
        situacao: clube.situacao,
        onPedido: () => lista.pedidoEnviado(clube.id),
        onReconciliar: () => lista.recarregar(true),
      })
    );
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Tela));
  });
  act(() => {
    button(tree, 'Pedir para entrar').props.onPress();
  });
  act(() => router.blur());
  assert.equal(posts[0].signal.aborted, true);
  // O cancelamento do transporte não desfaz a transação que já chegou ao servidor.
  situacao = 'pendente';
  await act(async () =>
    posts[0].resolve({ id: 'pedido', situacao: 'pendente' })
  );
  assert.deepEqual(toasts, []);
  assert.equal(lista.estado.clubes[0].situacao, 'nenhuma');
  await act(async () => router.focus());
  assert.equal(button(tree, 'Pedido enviado').props.disabled, true);
  assert.equal(button(tree, 'Pedido enviado').props.loading, false);
  situacao = 'nenhuma';
  await act(async () => router.focus());
  act(() => {
    button(tree, 'Pedir para entrar').props.onPress();
  });
  assert.equal(posts.length, 2);
  act(() => router.blur());
  await act(async () =>
    posts[1].reject(
      new client.ApiError(409, {
        code: 'JA_E_MEMBRO',
        message: 'Resposta tardia.',
      })
    )
  );
  assert.deepEqual(toasts, []);
  act(() => tree.unmount());
});

test('proteção conserva rascunho real do quiz e suspende efeitos até confirmar novamente o cargo', async () => {
  const router = focusRouter(),
    requests = [];
  let efeitos = 0,
    renderizacoes = 0;
  const { AcessoClube } = load('features/clube/AcessoClube.tsx', {
    ...ui(router),
    './api': {
      buscarClube: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, signal });
        return d.promise;
      },
    },
  });
  const noop = () => {};
  const { ConfigurarQuizScreen } = load(
    'features/clube/quiz/ConfigurarQuizScreen.tsx',
    {
      ...ui(router),
      'react-native': {
        ...native,
        TextInput: 'TextInput',
        KeyboardAvoidingView: 'KeyboardAvoidingView',
        Platform: { OS: 'ios' },
      },
      'react-native-safe-area-context': {
        useSafeAreaInsets: () => ({ top: 0, bottom: 0 }),
      },
      '@/components/Card': {
        Card: ({ children }) => React.createElement('Card', {}, children),
      },
      '@/components/Slider': { Slider: 'Slider' },
      '@/features/clube/useClube': {
        useClube: () => ({
          clube: {
            situacao: 'sucesso',
            dados: club('membro', 'a', 'administrador'),
          },
          recarregar: noop,
        }),
      },
    }
  );
  function Conteudo() {
    renderizacoes++;
    React.useEffect(() => {
      efeitos++;
      return () => efeitos--;
    }, []);
    return React.createElement(ConfigurarQuizScreen, { clubeId: 'a' });
  }
  let tree;
  act(() => {
    tree = create(
      React.createElement(
        AcessoClube,
        { clubeId: 'a', administrador: true },
        React.createElement(Conteudo)
      )
    );
  });
  assert.equal(renderizacoes, 0);
  await act(async () =>
    requests[0].resolve(club('membro', 'a', 'administrador'))
  );
  const input = () => tree.root.findByType('TextInput');
  act(() => {
    input().props.onChangeText('Quiz em edição');
    tree.root.findByType('Slider').props.onValueChange(250);
  });
  act(() => router.blur());
  assert.equal(efeitos, 0);
  assert.equal(visiveis(tree, 'TextInput').length, 0);
  act(() => router.focus());
  assert.equal(efeitos, 0);
  assert.equal(visiveis(tree, 'TextInput').length, 0);
  await act(async () =>
    requests[1].resolve(club('membro', 'a', 'administrador'))
  );
  assert.equal(efeitos, 1);
  assert.equal(input().props.value, 'Quiz em edição');
  assert.equal(tree.root.findByType('Slider').props.value, 250);
  act(() => router.blur());
  act(() => router.focus());
  await act(async () => requests[2].resolve(club('membro')));
  assert.equal(efeitos, 0);
  assert.equal(tree.root.findAllByType('TextInput').length, 0);
  act(() => tree.unmount());
});

test('Página aborta a leitura do detalhe ao perder foco e não aplica a resposta antiga', async () => {
  const router = focusRouter(),
    requests = [];
  const hooks = load('features/clube/useClube.ts', {
    '@/api/client': client,
    './api': {
      buscarClube: (id, signal) => {
        const d = deferred();
        requests.push({ ...d, signal });
        return d.promise;
      },
    },
  });
  const { PaginaClubeScreen } = load('features/clube/PaginaClubeScreen.tsx', {
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
  });
  let tree;
  act(() => {
    tree = create(React.createElement(PaginaClubeScreen, { clubeId: 'a' }));
  });
  act(() => router.blur());
  assert.equal(requests[0].signal.aborted, true);
  await act(async () =>
    requests[0].resolve(club('membro', 'a', 'administrador'))
  );
  assert.equal(button(tree, 'Registrar leitura').props.disabled, true);
  act(() => router.focus());
  assert.equal(requests.length, 2);
  await act(async () => requests[1].resolve(club('pendente')));
  assert.equal(
    tree.root.findByType('EntradaClubeButton').props.situacao,
    'pendente'
  );
  act(() => tree.unmount());
});
