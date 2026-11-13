const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, deps) {
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

const noop = () => {};
const linhas = ['eu', 'outro'].map((id, index) => ({
  posicao: index + 1,
  pontos: 10 - index,
  usuario: { id, nome: id, avatarUrl: null },
}));

async function montar(tela) {
  const requests = [],
    navegacoes = [],
    erros = [],
    cleanups = new Set();
  const router = {
    push: (route) => navegacoes.push(['push', route]),
    navigate: (route) => navegacoes.push(['navigate', route]),
    canGoBack: () => true,
    back: () => navegacoes.push(['back']),
    replace: (route) => navegacoes.push(['replace', route]),
  };
  const deps = {
    react: React,
    'expo-router': {
      useRouter: () => router,
      useFocusEffect: (cb) =>
        React.useEffect(() => {
          const cleanup = cb();
          if (cleanup) cleanups.add(cleanup);
          return () => {
            cleanups.delete(cleanup);
            cleanup?.();
          };
        }, [cb]),
    },
    'react-native': {
      ...Object.fromEntries(
        [
          'View',
          'Text',
          'ScrollView',
          'Pressable',
          'FlatList',
          'ActivityIndicator',
        ].map((name) => [name, name])
      ),
      StyleSheet: { create: (value) => value },
    },
    'react-native-svg': {
      __esModule: true,
      default: 'Svg',
      Path: 'Path',
      Rect: 'Rect',
    },
    '@/api/client': { ApiError: Error },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showErrorToast: (message) => erros.push(message),
      }),
    },
    '@/features/perfil/api': {
      buscarUsuarioAtual: (signal) =>
        new Promise((resolve, reject) =>
          requests.push({ signal, resolve, reject })
        ),
    },
    '@/features/livro/api': {},
    '@/features/quiz/api': {},
    '@/features/quiz/retornoDoQuiz': {
      consumirRetorno: () => null,
      textoDoResumo: noop,
    },
    '@/features/progresso/RegistrarProgressoSheet': {
      RegistrarProgressoSheet: 'RegistrarProgressoSheet',
    },
    './api': {},
    './EntradaClubeButton': { EntradaClubeButton: 'EntradaClubeButton' },
    './RankingCard': {
      RankingCard: 'RankingCard',
      FaixaQuadriculada: 'FaixaQuadriculada',
    },
    './useRankingClube': {
      useRankingClube: () => ({
        ranking: {
          situacao: 'sucesso',
          dados: { clube: { nome: 'Clube' }, ranking: linhas },
        },
        recarregar: noop,
        carregarMais: noop,
      }),
    },
    './useClube': {
      useClube: () => ({
        clube: {
          situacao: 'sucesso',
          dados: {
            id: 'clube',
            nome: 'Clube',
            situacao: 'membro',
            meuCargo: 'membro',
            livroAtual: { titulo: 'Livro' },
            ranking: linhas,
          },
        },
        recarregar: noop,
        atualizar: noop,
        cancelarCarregamento: noop,
      }),
    },
  };
  for (const name of [
    'AppHeader',
    'Card',
    'EmptyState',
    'PrimaryButton',
    'BottomSheet',
  ]) {
    deps[`@/components/${name}`] = { [name]: name };
  }
  deps['@/components/icons/BookIcon'] = { BookIcon: 'BookIcon' };
  deps['@/components/RankingRow'] = {
    RankingRow: 'RankingRow',
    formatRankingLabel: (position, name) => `${position}º lugar: ${name}`,
    formatRankingPoints: (points) => `${points} pontos`,
  };
  deps['./useAbrirPerfilDoRanking'] = load(
    'features/clube/useAbrirPerfilDoRanking.ts',
    deps
  );
  const componentName =
    tela === 'pagina' ? 'PaginaClubeScreen' : 'RankingClubeScreen';
  const Component = load(`features/clube/${componentName}.tsx`, deps)[
    componentName
  ];
  let tree;
  const render = (clubeId) => React.createElement(Component, { clubeId });
  await act(async () => {
    tree = create(render('clube'));
  });
  return {
    requests,
    navegacoes,
    erros,
    tocar: (id) => {
      if (tela === 'pagina')
        tree.root.findByType('RankingCard').props.onLinhaPress(id);
      else
        tree.root
          .findByType('FlatList')
          .props.renderItem({
            item: linhas.find((linha) => linha.usuario.id === id),
            index: 0,
          })
          .props.onPress();
    },
    blur: () =>
      act(() => {
        cleanups.forEach((cleanup) => cleanup());
      }),
    trocarClube: () => act(() => tree.update(render('outro-clube'))),
    fechar: () => act(() => tree.unmount()),
  };
}

for (const tela of ['pagina', 'completo']) {
  for (const id of ['eu', 'outro']) {
    test(`${tela}: tocar em ${id} abre o destino correto após consultar /api/me`, async () => {
      const h = await montar(tela);
      try {
        act(() => h.tocar(id));
        assert.deepEqual(h.navegacoes, []);
        await act(async () => h.requests[0].resolve({ id: 'eu' }));
        assert.deepEqual(
          h.navegacoes,
          id === 'eu'
            ? [['navigate', '/(tabs)/(perfil)/perfil']]
            : [
                [
                  'push',
                  {
                    pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
                    params: { usuarioId: 'outro' },
                  },
                ],
              ]
        );
        assert.deepEqual(h.erros, []);
      } finally {
        h.fechar();
      }
    });
  }

  test(`${tela}: toques repetidos não duplicam consulta e falha permite nova tentativa`, async () => {
    const h = await montar(tela);
    try {
      act(() => {
        h.tocar('outro');
        h.tocar('eu');
      });
      assert.equal(h.requests.length, 1);
      await act(async () => h.requests[0].reject(new Error('offline')));
      assert.deepEqual(h.navegacoes, []);
      assert.deepEqual(h.erros, [
        'Não foi possível abrir o perfil. Tente novamente.',
      ]);
      act(() => h.tocar('eu'));
      assert.equal(h.requests.length, 2);
      await act(async () => h.requests[1].resolve({ id: 'eu' }));
      assert.deepEqual(h.navegacoes, [['navigate', '/(tabs)/(perfil)/perfil']]);
    } finally {
      h.fechar();
    }
  });

  for (const saida of ['blur', 'trocarClube', 'fechar']) {
    test(`${tela}: ${saida} cancela consulta e ignora retorno atrasado`, async () => {
      const h = await montar(tela);
      try {
        act(() => h.tocar('outro'));
        h[saida]();
        assert.equal(h.requests[0].signal.aborted, true);
        await act(async () => h.requests[0].resolve({ id: 'eu' }));
        assert.deepEqual(h.navegacoes, []);
        assert.deepEqual(h.erros, []);
      } finally {
        if (saida !== 'fechar') h.fechar();
      }
    });
  }
}

test('RankingCard informa o ID da linha tocada, inclusive em empate', async () => {
  const ids = [];
  const { RankingCard } = load('features/clube/RankingCard.tsx', {
    'react-native': {
      View: 'View',
      Text: 'Text',
      Pressable: 'Pressable',
      ActivityIndicator: 'ActivityIndicator',
      StyleSheet: { create: (value) => value },
    },
    '@/components/Card': { Card: 'Card' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/RankingRow': {
      RankingRow: 'RankingRow',
      formatRankingLabel: (p, n) => `${p}º lugar: ${n}`,
      formatRankingPoints: (p) => `${p} pontos`,
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
  });
  let tree;
  await act(async () => {
    tree = create(
      React.createElement(RankingCard, {
        ranking: linhas.map((linha) => ({ ...linha, posicao: 1 })),
        onLinhaPress: (id) => ids.push(id),
      })
    );
  });
  try {
    act(() =>
      tree.root.findAllByType('Pressable').forEach((row) => row.props.onPress())
    );
    assert.deepEqual(ids, ['eu', 'outro']);
  } finally {
    act(() => tree.unmount());
  }
});
