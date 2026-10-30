const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, dependencies = {}) {
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
    (name) => dependencies[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}
const client = load('api/client.ts');
const FALLBACK = 'Não foi possível carregar. Tente novamente.';

for (const [name, body] of [
  ['sem corpo', ''],
  ['não JSON', '<html>Bad Gateway</html>'],
  ['null', 'null'],
  ['array', '[]'],
  ['sem message', '{"code":"INTERNAL_ERROR"}'],
  ['message vazia', '{"code":"INTERNAL_ERROR","message":""}'],
  ['message em branco', '{"code":"INTERNAL_ERROR","message":"   "}'],
  ['message não textual', '{"code":"INTERNAL_ERROR","message":500}'],
  ['sem code (intermediário)', '{"message":"upstream connection refused"}'],
  ['code em branco', '{"code":"  ","message":"diagnóstico"}'],
  ['code não textual', '{"code":500,"message":"diagnóstico"}'],
]) {
  test(`cliente: ${name} usa fallback sem perder status/code`, async () => {
    const original = global.fetch;
    global.fetch = async () => new Response(body, { status: 500 });
    try {
      await assert.rejects(client.apiGet('/teste'), (error) => {
        assert.ok(error instanceof client.ApiError);
        assert.equal(error.status, 500);
        assert.equal(error.publicMessage, null);
        assert.equal(client.mensagemErroCarregamento(error), FALLBACK);
        if (body.includes('INTERNAL_ERROR'))
          assert.equal(error.code, 'INTERNAL_ERROR');
        return true;
      });
    } finally {
      global.fetch = original;
    }
  });
}

test('cliente: envelope reconhecido preserva mensagem pública e tratamento programático', async () => {
  const original = global.fetch;
  const message = '  Siga a comunidade para comentar.  ';
  global.fetch = async () =>
    Response.json({ code: 'SO_QUEM_SEGUE_COMENTA', message }, { status: 403 });
  try {
    await assert.rejects(client.apiGet('/teste'), (error) => {
      assert.ok(error instanceof Error);
      assert.equal(error.name, 'ApiError');
      assert.equal(error.status, 403);
      assert.equal(error.code, 'SO_QUEM_SEGUE_COMENTA');
      assert.equal(error.message, message);
      assert.equal(error.publicMessage, message.trim());
      assert.equal(client.mensagemErroCarregamento(error), message.trim());
      return true;
    });
    // Reconhecimento pelo contrato, sem lista fechada de códigos ou frases.
    assert.equal(
      client.mensagemErroCarregamento(
        new client.ApiError(409, {
          code: 'NOVO_CODIGO_DO_BACKEND',
          message: 'Mensagem pública nova.',
        })
      ),
      'Mensagem pública nova.'
    );
  } finally {
    global.fetch = original;
  }
});

test('cliente: rede e JSON inválido em resposta de sucesso não expõem Error.message', async () => {
  const original = global.fetch;
  try {
    for (const fetch of [
      async () => {
        throw new TypeError('Network request failed: endereço interno');
      },
      async () => new Response('não JSON', { status: 200 }),
    ]) {
      global.fetch = fetch;
      await assert.rejects(client.apiGet('/teste'), (error) => {
        assert.equal(client.mensagemErroCarregamento(error), FALLBACK);
        return true;
      });
    }
    for (const error of [
      null,
      undefined,
      new Error('segredo'),
      { message: 'proxy' },
    ]) {
      assert.equal(client.mensagemErroCarregamento(error), FALLBACK);
    }
  } finally {
    global.fetch = original;
  }
});

test('cliente: renovação de token, perda de sessão e código 401 permanecem intactos', async () => {
  const original = global.fetch;
  const events = [];
  client.configurarAutenticacao({
    obterToken: async () => 'antigo',
    renovarToken: async () => {
      events.push('renovar');
      return 'novo';
    },
    aoPerderSessao: async () => {
      events.push('sair');
    },
  });
  global.fetch = async (_, init) => {
    events.push(init.headers.get('Authorization'));
    return Response.json(
      {
        code: 'UNAUTHORIZED',
        message: 'Sessão inválida ou expirada. Entre de novo.',
      },
      { status: 401 }
    );
  };
  try {
    await assert.rejects(client.apiGet('/teste'), (error) => {
      assert.equal(error.status, 401);
      assert.equal(error.code, 'UNAUTHORIZED');
      assert.equal(
        error.message,
        'Sessão inválida ou expirada. Entre de novo.'
      );
      return true;
    });
    assert.deepEqual(events, [
      'Bearer antigo',
      'renovar',
      'Bearer novo',
      'sair',
    ]);
  } finally {
    client.configurarAutenticacao(null);
    global.fetch = original;
  }
});

const cases = [
  [
    'feed/useFeedComunidades.ts',
    'useFeedComunidades',
    [],
    'feed',
    'buscarFeedComunidades',
  ],
  [
    'comunidade/useComunidade.ts',
    'useComunidade',
    ['id'],
    'comunidade',
    'buscarComunidade',
  ],
  [
    'biblioteca/useBiblioteca.ts',
    'useBiblioteca',
    [],
    'biblioteca',
    'buscarBiblioteca',
  ],
  ['clube/useClube.ts', 'useClube', ['id'], 'clube', 'buscarClube'],
  [
    'clube/useRankingClube.ts',
    'useRankingClube',
    ['id'],
    'ranking',
    'buscarRankingClube',
  ],
  [
    'forum/useForumClube.ts',
    'useForumClube',
    ['id'],
    'forum',
    'buscarForumDoClube',
  ],
  [
    'livro/useLivroDetalhe.ts',
    'useLivroDetalhe',
    ['id'],
    'estado',
    'buscarDetalhe',
  ],
  ['perfil/usePerfil.ts', 'usePerfil', [], 'estado', 'buscarPerfil'],
  [
    'seguidores/useListaSeguidores.ts',
    'useListaSeguidores',
    ['id', 'seguidores'],
    'carga',
    'buscarLista',
  ],
  [
    'conquistas/useMinhasConquistas.ts',
    'useMinhasConquistas',
    [],
    'estado',
    'buscarMinhasConquistas',
  ],
  ['mascotes/useMascotes.ts', 'useMascotes', [], 'estado', 'buscarMascotes'],
  [
    'desafios/useDesafiosSistema.ts',
    'useDesafiosSistema',
    [],
    'estado',
    'consultarDesafiosSistema',
  ],
  [
    'desafios/useEscolherLivro.ts',
    'useEstante',
    [],
    'estado',
    'buscarBiblioteca',
  ],
  [
    'desafios/useDesafiarAmigo.ts',
    'useOponentes',
    [''],
    'estado',
    'buscarOponentes',
  ],
  ['busca/useBusca.ts', 'useBusca', ['livro', 'livros'], 'busca', 'buscar'],
  [
    'adicionarLivro/useBuscaCatalogo.ts',
    'useBuscaCatalogo',
    ['livro'],
    'busca',
    'buscarCatalogo',
  ],
  [
    'comentarios/useComentarios.ts',
    'useComentarios',
    ['post'],
    'carga',
    'listarComentarios',
  ],
  [
    'chat/useChatClube.ts',
    'useChatClube',
    ['id'],
    'chat',
    'buscarMensagensDoClube',
  ],
];
for (const [file, hookName, args, stateKey, requestName] of cases) {
  test(`${hookName}: fallback, retry do carregamento e mensagem pública`, async () => {
    const requests = [];
    const api = {
      [requestName]: (...args) =>
        new Promise((resolve, reject) =>
          requests.push({ args, resolve, reject })
        ),
      buscarPerfil: async () => ({ nome: 'Pessoa' }),
      buscarUsuarioAtual: async () => ({ id: 'eu' }),
      normalizarTermoOponente: (termo) => termo.trim(),
    };
    // usePerfil precisa manter sua própria chamada pendente.
    api[requestName] = (...args) =>
      new Promise((resolve, reject) =>
        requests.push({ args, resolve, reject })
      );
    const deps = {
      '@/api/client': client,
      'expo-router': {
        useFocusEffect: (effect) => React.useEffect(effect, [effect]),
      },
      './api': api,
      './sistemaApi': api,
      '@/features/biblioteca/api': api,
      '@/features/perfil/api': api,
      '@/features/posts/api': {},
      './model': {},
    };
    const hooks = load(`features/${file}`, deps);
    let current;
    function Probe() {
      current = hooks[hookName](...args);
      return null;
    }
    let tree;
    const waitDebounce = () =>
      new Promise((resolve) => setTimeout(resolve, 320));
    try {
      await act(async () => {
        tree = create(React.createElement(Probe));
      });
      if (!requests.length) await act(waitDebounce);
      assert.equal(requests.length, 1);
      await act(async () => requests[0].reject(new client.ApiError(500, {})));
      assert.equal(current[stateKey].situacao, 'erro');
      assert.equal(current[stateKey].mensagem, FALLBACK);
      await act(async () => {
        current.recarregar();
      });
      if (requests.length === 1) await act(waitDebounce);
      assert.equal(requests.length, 2);
      await act(async () =>
        requests[1].reject(
          new client.ApiError(503, {
            code: 'EXTERNAL_SERVICE_UNAVAILABLE',
            message: 'Serviço indisponível no momento.',
          })
        )
      );
      assert.equal(
        current[stateKey].mensagem,
        'Serviço indisponível no momento.'
      );
    } finally {
      if (tree) await act(async () => tree.unmount());
    }
  });
}

const theme = { ...require('../src/theme/tokens'), textStyles: {} };
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (styles) => styles },
};
function screenDeps(back) {
  return {
    '@/api/client': client,
    'react-native': native,
    '@/theme': theme,
    'expo-router': { useRouter: () => ({ back, canGoBack: () => true }) },
    'react-native-safe-area-context': {
      useSafeAreaInsets: () => ({ top: 24, bottom: 0 }),
    },
    'expo-image': { Image: 'Image' },
    'react-native-svg': { __esModule: true, default: 'Svg', Path: 'Path' },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/EmptyState': {
      EmptyState: ({ action, ...props }) =>
        React.createElement('EmptyState', props, action),
    },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/PostCard': { PostCard: 'PostCard' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/SegmentedTabs': { SegmentedTabs: 'SegmentedTabs' },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/toast-provider': { useToastContext: () => ({}) },
    '@/features/comentarios/ComentariosSheet': {
      ComentariosSheet: 'ComentariosSheet',
    },
    '@/features/livro/model': {},
  };
}

test('comunidade: erro mantém hero/voltar, retry correto e transição para loading', async () => {
  let back = 0,
    calls = 0;
  let rejectRetry;
  const request = () => {
    calls++;
    if (calls === 1) return Promise.reject(new Error('diagnóstico de rede'));
    return new Promise((_, reject) => {
      rejectRetry = reject;
    });
  };
  const { useComunidade } = load('features/comunidade/useComunidade.ts', {
    '@/api/client': client,
    '@/features/posts/api': {},
    './api': { buscarComunidade: request },
  });
  const { ComunidadeScreen } = load(
    'features/comunidade/ComunidadeScreen.tsx',
    {
      ...screenDeps(() => back++),
      // A comunidade volta pelo useVoltar (RQ-140), não direto pelo router.
      '@/components/useVoltar': { useVoltar: () => () => back++ },
      './useComunidade': { useComunidade },
      './ComunidadeScreen.styles': load(
        'features/comunidade/ComunidadeScreen.styles.ts',
        {
          'react-native': native,
          '@/theme': theme,
        }
      ),
    }
  );
  let tree;
  try {
    await act(async () => {
      tree = create(
        React.createElement(ComunidadeScreen, {
          comunidadeId: 'id',
          origem: '/buscar',
        })
      );
    });
    assert.equal(calls, 1);
    assert.equal(tree.root.findByType('EmptyState').props.message, FALLBACK);
    assert.equal(tree.root.findAllByType('Svg').length, 1);
    tree.root.findByType('Pressable').props.onPress();
    assert.equal(back, 1);
    const retry = tree.root.findByType('PrimaryButton');
    assert.equal(retry.props.label, 'Tentar de novo');
    await act(async () => retry.props.onPress());
    assert.equal(calls, 2);
    assert.equal(tree.root.findAllByType('EmptyState').length, 0);
    assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
    await act(async () => rejectRetry(new Error('offline')));
    assert.equal(tree.root.findByType('EmptyState').props.message, FALLBACK);
    assert.equal(
      tree.root.findByType('Pressable').props.accessibilityLabel,
      'Voltar'
    );
  } finally {
    if (tree) await act(async () => tree.unmount());
  }
});

test('feed: erro mantém cabeçalho e botão refaz somente o carregamento do feed', async () => {
  let calls = 0;
  const { useFeedComunidades } = load('features/feed/useFeedComunidades.ts', {
    '@/api/client': client,
    './api': {
      buscarFeedComunidades: async () => {
        calls++;
        throw new Error('offline');
      },
    },
  });
  const { FeedComunidadesView } = load(
    'features/feed/FeedComunidadesView.tsx',
    {
      ...screenDeps(() => {}),
      './ClubeCard': { ClubeCard: 'ClubeCard' },
    }
  );
  function App() {
    return React.createElement(FeedComunidadesView, useFeedComunidades());
  }
  let tree;
  try {
    await act(async () => {
      tree = create(React.createElement(App));
    });
    assert.equal(tree.root.findByType('EmptyState').props.message, FALLBACK);
    assert.equal(tree.root.findAllByType('AppHeader').length, 1);
    const retry = tree.root.findByType('PrimaryButton');
    assert.equal(retry.props.label, 'Tentar de novo');
    await act(async () => retry.props.onPress());
    assert.equal(calls, 2);
  } finally {
    if (tree) await act(async () => tree.unmount());
  }
});
