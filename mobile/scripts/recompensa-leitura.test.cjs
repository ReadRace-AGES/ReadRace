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
        target: ts.ScriptTarget.ES2022,
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

const { linhasDaRecompensa } = load('features/progresso/recompensa.ts');

const semNada = {
  xpPaginas: 0,
  xpConclusao: 0,
  sequenciaDias: 3,
  nivel: 4,
  subiuDeNivel: false,
  conquistasDesbloqueadas: [],
};

test('XP de páginas com a sequência de dias', () => {
  assert.deepEqual(linhasDaRecompensa({ ...semNada, xpPaginas: 12 }), [
    '+12 XP · 3 dias de sequência',
  ]);
});

test('sequência de 1 dia no singular', () => {
  assert.deepEqual(
    linhasDaRecompensa({ ...semNada, xpPaginas: 5, sequenciaDias: 1 }),
    ['+5 XP · 1 dia de sequência']
  );
});

test('sem XP e sem nada novo não há linha nenhuma', () => {
  assert.deepEqual(linhasDaRecompensa(semNada), []);
});

test('tudo na mesma leitura vem numa linha por item, na ordem combinada', () => {
  assert.deepEqual(
    linhasDaRecompensa({
      xpPaginas: 40,
      xpConclusao: 150,
      sequenciaDias: 7,
      nivel: 5,
      subiuDeNivel: true,
      conquistasDesbloqueadas: [
        { id: 'a', nome: 'Mil Páginas', xp: 200 },
        { id: 'b', nome: 'Leitor Dedicado', xp: 100 },
      ],
    }),
    [
      '+40 XP · 7 dias de sequência',
      'Livro concluído! +150 XP',
      'Você subiu para o nível 5!',
      'Conquista desbloqueada: Mil Páginas',
      'Conquista desbloqueada: Leitor Dedicado',
    ]
  );
});

test('página repetida sem XP ainda avisa o que mais foi ganho', () => {
  assert.deepEqual(
    linhasDaRecompensa({
      ...semNada,
      conquistasDesbloqueadas: [{ id: 'c', nome: 'Competidor', xp: 100 }],
    }),
    ['Conquista desbloqueada: Competidor']
  );
});

function carregarProvider() {
  const theme = {
    colors: { primary: '#000' },
    radius: { pill: 9999, lg: 16 },
    shadows: { floating: {} },
    spacing: { 1: 4, 3: 12, 5: 20, 6: 24 },
  };
  const animated = {
    Value: class {
      constructor(value) {
        this.value = value;
      }
    },
    View: 'AnimatedView',
    timing: () => ({ start: (callback) => callback?.({ finished: true }) }),
  };
  const toast = load('components/toast.tsx', {
    'react-native': {
      View: 'View',
      Text: 'Text',
      StyleSheet: { create: (styles) => styles },
      Animated: animated,
    },
    'react-native-safe-area-context': {
      useSafeAreaInsets: () => ({ bottom: 0 }),
    },
    '@/theme': theme,
  });
  const provider = load('components/toast-provider.tsx', {
    'expo-router': { usePathname: () => '/livro' },
    './toast': toast,
  });
  return { ...toast, ...provider };
}

test('a duração cresce com o número de linhas', () => {
  const { duracaoDoAviso } = carregarProvider();
  assert.equal(duracaoDoAviso(1), 2500);
  assert.equal(duracaoDoAviso(4), 7000);
});

test('o aviso de sucesso mostra uma linha por item e some no tempo das linhas', (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { ToastProvider, useToastContext, Toast } = carregarProvider();
  let controles;
  function Probe() {
    controles = useToastContext();
    return null;
  }
  let tree;
  act(() => {
    tree = create(
      React.createElement(ToastProvider, null, React.createElement(Probe))
    );
  });
  try {
    const textos = () =>
      tree.root.findAllByType('Text').map((n) => n.props.children);

    act(() => controles.showSuccessToast([]));
    assert.equal(tree.root.findByType(Toast).props.visible, false);

    const linhas = ['+10 XP · 2 dias de sequência', 'Livro concluído! +150 XP'];
    act(() => controles.showSuccessToast(linhas));
    assert.equal(tree.root.findByType(Toast).props.visible, true);
    assert.deepEqual(textos(), linhas);

    // 2 linhas = 4 s: depois dos 2,5 s do aviso comum ele continua na tela.
    act(() => t.mock.timers.tick(2500));
    assert.equal(tree.root.findByType(Toast).props.visible, true);
    act(() => t.mock.timers.tick(1500));
    assert.equal(tree.root.findByType(Toast).props.visible, false);
  } finally {
    act(() => tree.unmount());
  }
});

function carregarPainel(registrarProgresso, eventos) {
  const { ApiError } = load('api/client.ts');
  const { ProgressoTimeoutError } = load('features/progresso/api.ts', {
    '@/api/client': {},
  });
  const { RegistrarProgressoSheet } = load(
    'features/progresso/RegistrarProgressoSheet.tsx',
    {
      'react-native': {
        View: 'View',
        Text: 'Text',
        TextInput: 'TextInput',
        Pressable: 'Pressable',
        StyleSheet: { create: (v) => v },
      },
      'react-native-svg': { __esModule: true, default: 'Svg', Path: 'Path' },
      '@/theme': {
        colors: {},
        radius: {},
        sizes: {},
        spacing: {},
        textStyles: {},
      },
      '@/api/client': { ApiError },
      './api': { registrarProgresso, ProgressoTimeoutError },
      './recompensa': load('features/progresso/recompensa.ts'),
      '@/components/BottomSheet': {
        BottomSheet: (props) =>
          props.visible
            ? React.createElement('BottomSheet', props, props.children)
            : null,
      },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
      '@/features/desafios/DesafiarAmigoIcons': { CloseIcon: 'CloseIcon' },
      '@/components/toast-provider': {
        useToastContext: () => ({
          showSuccessToast: (linhas) => eventos.push(['aviso', linhas]),
        }),
      },
    }
  );
  return RegistrarProgressoSheet;
}

async function registrarNoPainel(RegistrarProgressoSheet, eventos, pagina) {
  let tree;
  await act(async () => {
    tree = create(
      React.createElement(RegistrarProgressoSheet, {
        visible: true,
        livroId: 'livro-1',
        totalPaginas: 300,
        paginaAtual: 0,
        onClose: () => eventos.push(['fechou']),
      })
    );
  });
  await act(async () =>
    tree.root.findByType('TextInput').props.onChangeText(String(pagina))
  );
  await act(async () =>
    tree.root
      .findAllByType('PrimaryButton')
      .find((n) => n.props.label === 'Atualizar Progresso')
      .props.onPress()
  );
  return tree;
}

test('o painel fecha e só depois avisa tudo o que a leitura rendeu', async () => {
  const eventos = [];
  const painel = carregarPainel(
    async () => ({
      paginaAtual: 300,
      paginaMaximaAlcancada: 300,
      totalPaginas: 300,
      percentual: 100,
      xpPaginas: 20,
      xpConclusao: 150,
      xpTotal: 170,
      concluido: true,
      sequenciaDias: 5,
      nivel: 6,
      xpDoUsuario: 2000,
      xpNoNivel: 10,
      xpDoNivel: 900,
      subiuDeNivel: true,
      conquistasDesbloqueadas: [{ id: 'x', nome: 'Mil Páginas', xp: 200 }],
    }),
    eventos
  );
  const tree = await registrarNoPainel(painel, eventos, 300);
  try {
    assert.deepEqual(eventos, [
      ['fechou'],
      [
        'aviso',
        [
          '+20 XP · 5 dias de sequência',
          'Livro concluído! +150 XP',
          'Você subiu para o nível 6!',
          'Conquista desbloqueada: Mil Páginas',
        ],
      ],
    ]);
  } finally {
    await act(async () => tree.unmount());
  }
});

test('quando o registro falha, o erro fica no painel e não há aviso de recompensa', async () => {
  const eventos = [];
  const painel = carregarPainel(async () => {
    throw new Error('rede');
  }, eventos);
  const tree = await registrarNoPainel(painel, eventos, 50);
  try {
    assert.deepEqual(eventos, []);
    assert.ok(
      tree.root
        .findAllByType('Text')
        .some(
          (n) =>
            n.props.children ===
            'Não foi possível registrar o progresso. Tente novamente.'
        )
    );
  } finally {
    await act(async () => tree.unmount());
  }
});

test('Detalhe do livro e Página do clube registram pelo mesmo painel', () => {
  for (const tela of [
    'features/livro/LivroDetalheScreen.tsx',
    'features/clube/PaginaClubeScreen.tsx',
  ]) {
    const fonte = fs.readFileSync(path.join(__dirname, '../src', tela), 'utf8');
    assert.match(
      fonte,
      /from '@\/features\/progresso\/RegistrarProgressoSheet'/,
      tela
    );
    assert.match(fonte, /<RegistrarProgressoSheet/, tela);
  }
});
