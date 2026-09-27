const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');

function load(relativePath, dependencies = {}) {
  const source = fs.readFileSync(
    path.join(__dirname, '..', relativePath),
    'utf8'
  );
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

const { aplicarProgresso, tempoRelativo } = load('src/features/livro/model.ts');

test('primeiro registro usa percentual do servidor e preserva livro e posts', () => {
  const detalhe = {
    livro: { id: '1', titulo: 'Livro', totalPaginas: 100 },
    progresso: null,
    posts: [{ id: 'post' }],
  };
  const resposta = {
    paginaAtual: 10,
    paginaMaximaAlcancada: 10,
    totalPaginas: 200,
    percentual: 6,
    concluido: false,
    xpTotal: 10,
  };
  const atualizado = aplicarProgresso(detalhe, resposta);
  assert.deepEqual(atualizado.progresso, {
    paginaAtual: 10,
    paginaMaximaAlcancada: 10,
    percentual: 6,
    concluido: false,
  });
  assert.equal(atualizado.livro.totalPaginas, 200);
  assert.equal(atualizado.livro.titulo, 'Livro');
  assert.equal(atualizado.posts, detalhe.posts);
  assert.equal(detalhe.progresso, null);
  assert.equal(detalhe.livro.totalPaginas, 100);
  assert.equal('xpTotal' in atualizado.progresso, false);
});

function renderDetalhe(dados) {
  const componentNames = [
    'AppHeader',
    'BookCover',
    'Card',
    'EmptyState',
    'PostCard',
    'PrimaryButton',
    'ProgressBar',
  ];
  const dependencies = {
    react: {
      useCallback: (fn) => fn,
      useState: (inicial) => [inicial, () => {}],
    },
    'expo-router': { useFocusEffect: () => {}, useRouter: () => ({}) },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showToast: () => {},
        showErrorToast: () => {},
      }),
    },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      Pressable: 'Pressable',
      ScrollView: 'ScrollView',
      Text: 'Text',
      View: 'View',
      StyleSheet: { create: (styles) => styles },
    },
    '@/theme': {
      bookCover: { detail: { height: 120 } },
      colors: {},
      radius: {},
      sizes: {},
      spacing: {},
      textStyles: {},
    },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/features/progresso/RegistrarProgressoSheet': {
      RegistrarProgressoSheet: 'RegistrarProgressoSheet',
    },
    './model': { tempoRelativo },
    './useLivroDetalhe': {
      useLivroDetalhe: () => ({ estado: { situacao: 'sucesso', dados } }),
    },
  };
  for (const name of componentNames)
    dependencies[`@/components/${name}`] = { [name]: name };
  const { LivroDetalheScreen } = load(
    'src/features/livro/LivroDetalheScreen.tsx',
    dependencies
  );
  const elements = [];
  function visit(node) {
    if (Array.isArray(node)) return node.forEach(visit);
    if (!node || typeof node !== 'object') return;
    elements.push(node);
    visit(node.props?.children);
  }
  visit(LivroDetalheScreen({ livroId: dados.livro.id }));
  return elements;
}

test('tela preserva gênero, ordem, autores e curtidas da resposta agregada', async () => {
  const dados = {
    livro: { id: '1', titulo: 'Livro', genero: 'Clássico', totalPaginas: 100 },
    progresso: null,
    posts: [
      {
        id: 'recente',
        autor: { nome: 'Ana', avatarUrl: null, sequenciaDias: 0 },
        texto: 'Primeiro',
        criadoEm: '2026-09-15T12:00:00Z',
        curtidas: 0,
      },
      {
        id: 'antigo',
        autor: { nome: 'Bruno', avatarUrl: 'avatar.png', sequenciaDias: 7 },
        texto: 'Segundo',
        criadoEm: '2026-09-14T12:00:00Z',
        curtidas: 12,
      },
    ],
  };
  let consultas = 0;
  const { buscarDetalhe } = load('src/features/livro/api.ts', {
    '@/api/client': {
      apiGet: async () => {
        consultas++;
        return dados;
      },
    },
  });
  const elements = renderDetalhe(
    await buscarDetalhe('1', new AbortController().signal)
  );
  assert.equal(consultas, 1);
  assert.ok(
    elements.some((e) => e.type === 'Text' && e.props.children === 'Clássico')
  );
  const posts = elements.filter((e) => e.type === 'PostCard');
  assert.deepEqual(
    posts.map((e) => [e.key, e.props.authorName, e.props.likes]),
    [
      ['recente', 'Ana', 0],
      ['antigo', 'Bruno', 12],
    ]
  );
  assert.equal(posts[0].props.authorPhotoUrl, null);
  assert.equal(posts[1].props.authorPhotoUrl, 'avatar.png');
  assert.equal(posts[1].props.authorStreak, '7 dias');
});

test('tela aceita gênero ausente e posts vazios', () => {
  const elements = renderDetalhe({
    livro: { id: '1', titulo: 'Livro', genero: null, totalPaginas: 100 },
    progresso: null,
    posts: [],
  });
  assert.equal(elements.filter((e) => e.type === 'PostCard').length, 0);
  assert.equal(elements.filter((e) => e.type === 'EmptyState').length, 1);
  assert.equal(
    elements.find((e) => e.type === 'ProgressBar').props.progress,
    0
  );
});

test('retrocesso preserva máxima e conclusão recebidas do backend', () => {
  const detalhe = {
    livro: { totalPaginas: 100 },
    progresso: { paginaAtual: 100 },
    posts: [],
  };
  const atualizado = aplicarProgresso(detalhe, {
    paginaAtual: 20,
    paginaMaximaAlcancada: 100,
    totalPaginas: 100,
    percentual: 20,
    concluido: true,
    xpTotal: 0,
  });
  assert.deepEqual(atualizado.progresso, {
    paginaAtual: 20,
    paginaMaximaAlcancada: 100,
    percentual: 20,
    concluido: true,
  });
});

test('tempo relativo cobre agora, minutos, horas, dias e datas inválidas', () => {
  const agora = Date.parse('2026-09-14T12:00:00Z');
  for (const [data, resultado] of [
    ['2026-09-14T12:01:00Z', 'agora'],
    ['2026-09-14T11:59:00Z', '1min atrás'],
    ['2026-09-14T09:00:00Z', '3h atrás'],
    ['2026-09-12T12:00:00Z', '2d atrás'],
    ['inválida', ''],
  ])
    assert.equal(tempoRelativo(data, agora), resultado);
});

test('consulta detalhe encaminha cancelamento e não envia userId', async () => {
  const signal = new AbortController().signal;
  const esperado = { livro: { id: '1' }, progresso: null, posts: [] };
  const { buscarDetalhe } = load('src/features/livro/api.ts', {
    '@/api/client': {
      apiGet: async (url, init) => {
        assert.equal(url, '/api/livros/id%2Fcom%20espa%C3%A7o');
        assert.deepEqual(init, { signal });
        return esperado;
      },
    },
  });
  assert.equal(await buscarDetalhe('id/com espaço', signal), esperado);
});
