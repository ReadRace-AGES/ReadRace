const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.ReactJSX,
    },
  });
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => dependencies[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}

const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (v) => v },
};
const theme = {
  bookCover: { detail: { height: 120 } },
  colors: {},
  radius: {},
  sizes: {},
  spacing: {},
  textStyles: {},
  typography: { fontFamily: {} },
};

const { ApiError } = load('src/api/client.ts');

test('LivroDetalheScreen: curtir é otimista, ignora toque duplo em andamento, reverte em falha com toast e persiste em sucesso', async () => {
  let resolveCurtir, rejectCurtir;
  let chamadasCurtir = 0;
  let chamadasDescurtir = 0;
  const postsApi = {
    curtir: () => {
      chamadasCurtir++;
      return new Promise((resolve, reject) => {
        resolveCurtir = resolve;
        rejectCurtir = reject;
      });
    },
    descurtir: () => {
      chamadasDescurtir++;
      return Promise.resolve({ curtidoPorMim: false, totalCurtidas: 1 });
    },
  };
  const model = load('src/features/livro/model.ts');
  const { useLivroDetalhe } = load('src/features/livro/useLivroDetalhe.ts', {
    '@/api/client': { ApiError },
    '@/features/posts/api': postsApi,
    './model': model,
    './api': {
      buscarDetalhe: async () => ({
        livro: { id: '1', titulo: 'Livro', totalPaginas: 100 },
        progresso: null,
        posts: [
          {
            id: 'p1',
            autor: { nome: 'Ana', avatarUrl: null, sequenciaDias: null },
            texto: 'oi',
            criadoEm: '2026-01-01T00:00:00Z',
            curtidas: 2,
            curtidoPorMim: false,
          },
        ],
      }),
    },
  });

  const erros = [];
  const dependencies = {
    'react-native': native,
    '@/theme': theme,
    './model': model,
    './useLivroDetalhe': { useLivroDetalhe },
    'expo-router': {
      useRouter: () => ({}),
      useFocusEffect: (callback) => React.useEffect(callback, [callback]),
    },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showToast: () => {},
        showErrorToast: (mensagem) => erros.push(mensagem),
      }),
    },
    '@/features/progresso/RegistrarProgressoSheet': {
      RegistrarProgressoSheet: 'RegistrarProgressoSheet',
    },
  };
  for (const name of [
    'AppHeader',
    'BookCover',
    'Card',
    'EmptyState',
    'PostCard',
    'PrimaryButton',
    'ProgressBar',
  ])
    dependencies[`@/components/${name}`] = { [name]: name };
  const { LivroDetalheScreen } = load(
    'src/features/livro/LivroDetalheScreen.tsx',
    dependencies
  );

  let renderer;
  await act(async () => {
    renderer = create(
      React.createElement(LivroDetalheScreen, { livroId: '1' })
    );
  });
  try {
    const post = () => renderer.root.findByType('PostCard');
    assert.equal(post().props.likes, 2);
    assert.equal(post().props.likedByMe, false);
    assert.equal(post().props.likeDisabled, false);

    let pendente;
    await act(async () => {
      pendente = post().props.onLikePress();
    });
    assert.equal(
      post().props.likes,
      3,
      'curtida otimista incrementa antes da resposta'
    );
    assert.equal(post().props.likedByMe, true);
    assert.equal(post().props.likeDisabled, true);
    assert.equal(chamadasCurtir, 1);

    await act(async () => {
      post().props.onLikePress();
    });
    assert.equal(
      chamadasCurtir,
      1,
      'toque duplo enquanto pendente não dispara nova chamada'
    );

    await act(async () => {
      rejectCurtir(
        new ApiError(500, {
          code: 'INTERNAL_ERROR',
          message: 'Falha ao curtir',
        })
      );
      await pendente;
    });
    assert.equal(post().props.likes, 2, 'falha reverte para o valor anterior');
    assert.equal(post().props.likedByMe, false);
    assert.equal(post().props.likeDisabled, false);
    assert.deepEqual(erros, ['Falha ao curtir']);

    await act(async () => {
      pendente = post().props.onLikePress();
    });
    assert.equal(chamadasCurtir, 2);
    await act(async () => {
      resolveCurtir({ curtidoPorMim: true, totalCurtidas: 3 });
      await pendente;
    });
    assert.equal(post().props.likes, 3, 'sucesso mantém o estado otimista');
    assert.equal(post().props.likedByMe, true);
    assert.equal(post().props.likeDisabled, false);

    await act(async () => {
      pendente = post().props.onLikePress();
    });
    assert.equal(post().props.likes, 2, 'descurtir otimista decrementa');
    assert.equal(post().props.likedByMe, false);
    await act(async () => {
      await pendente;
    });
    assert.equal(chamadasDescurtir, 1);
  } finally {
    await act(async () => renderer.unmount());
  }
});

test('useForumClube.alternarCurtida: otimista e reverte em falha, propagando o erro para quem chamou', async () => {
  let resolveCurtir, rejectCurtir;
  let chamadas = 0;
  const postsApi = {
    curtir: () => {
      chamadas++;
      return new Promise((resolve, reject) => {
        resolveCurtir = resolve;
        rejectCurtir = reject;
      });
    },
    descurtir: () =>
      Promise.resolve({ curtidoPorMim: false, totalCurtidas: 4 }),
  };
  const { useForumClube } = load('src/features/forum/useForumClube.ts', {
    '@/api/client': { ApiError },
    '@/features/posts/api': postsApi,
    './api': {
      buscarForumDoClube: async () => ({
        clube: {
          id: 'c1',
          nome: 'Clube',
          livroAtual: { titulo: 'L', autor: 'A', capaUrl: null },
        },
        posts: [
          {
            id: 'p1',
            autor: {
              id: 'u1',
              nome: 'Autor',
              avatarUrl: null,
              sequenciaDias: null,
            },
            publicadoEm: '2026-01-01T00:00:00Z',
            texto: 'oi',
            totalCurtidas: 5,
            curtidoPorMim: false,
          },
        ],
      }),
    },
  });

  let ultimo;
  function Probe({ clubeId }) {
    ultimo = useForumClube(clubeId);
    return null;
  }
  let renderer;
  await act(async () => {
    renderer = create(React.createElement(Probe, { clubeId: 'c1' }));
  });
  try {
    await act(async () => {});
    assert.equal(ultimo.forum.situacao, 'sucesso');
    assert.equal(ultimo.forum.dados.posts[0].totalCurtidas, 5);

    let pendente;
    await act(async () => {
      pendente = ultimo.alternarCurtida('p1');
    });
    assert.equal(ultimo.forum.dados.posts[0].totalCurtidas, 6);
    assert.equal(ultimo.forum.dados.posts[0].curtidoPorMim, true);
    assert.equal(chamadas, 1);

    await act(async () => {
      rejectCurtir(
        new ApiError(500, {
          code: 'INTERNAL_ERROR',
          message: 'Falha ao curtir',
        })
      );
      await assert.rejects(pendente, /Falha ao curtir/);
    });
    assert.equal(
      ultimo.forum.dados.posts[0].totalCurtidas,
      5,
      'reverte após falha'
    );
    assert.equal(ultimo.forum.dados.posts[0].curtidoPorMim, false);
  } finally {
    await act(async () => renderer.unmount());
  }
});
