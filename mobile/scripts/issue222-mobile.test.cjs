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
const tokens = require('../src/theme/tokens');
const theme = load('theme/index.ts', {
  './tokens': { __esModule: true, default: tokens },
  '@expo-google-fonts/inter': {},
});
const noop = () => {};
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  TextInput: 'TextInput',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  KeyboardAvoidingView: 'KeyboardAvoidingView',
  TouchableOpacity: 'TouchableOpacity',
  Platform: { OS: 'ios' },
  useWindowDimensions: () => ({ width: 320, height: 640 }),
  FlatList: ({ data, renderItem, ...props }) =>
    React.createElement(
      'FlatList',
      props,
      data.map((item, index) =>
        React.createElement(
          React.Fragment,
          { key: item.usuario?.id ?? item.id },
          renderItem({ item, index })
        )
      )
    ),
  StyleSheet: {
    create: (style) => style,
    absoluteFill: { position: 'absolute' },
  },
};
const base = {
  'react-native': native,
  '@/theme': theme,
  '@/api/client': load('api/client.ts'),
  '@/components/LinhaUsuario': { LinhaUsuario: 'LinhaUsuario' },
  'expo-router': {
    useRouter: () => ({
      back: noop,
      canGoBack: () => true,
      push: noop,
      dismissTo: noop,
    }),
    useLocalSearchParams: () => ({}),
    useFocusEffect: (effect) => React.useEffect(effect, [effect]),
  },
  'react-native-safe-area-context': {
    useSafeAreaInsets: () => ({ top: 24, bottom: 0 }),
  },
  'expo-image': { Image: 'Image' },
  'react-native-svg': {
    __esModule: true,
    default: 'Svg',
    Path: 'Path',
    Circle: 'Circle',
  },
  '@react-native-masked-view/masked-view': {
    __esModule: true,
    default: 'MaskedView',
  },
  'expo-linear-gradient': { LinearGradient: 'LinearGradient' },
  '@/components/avatar': { Avatar: 'Avatar' },
  '@/components/BookCover': { BookCover: 'BookCover' },
  '@/components/EmptyState': { EmptyState: 'EmptyState' },
  '@/components/ListItem': { ListItem: 'ListItem' },
  '@/components/toast-provider': {
    useToastContext: () => ({ showToast: noop, showErrorToast: noop }),
  },
  '@/features/livro/model': {},
};
for (const icon of [
  'BookIcon',
  'SearchIcon',
  'UsersIcon',
  'TrophyIcon',
  'RematchIcon',
  'SadFaceIcon',
  'GoogleIcon',
]) {
  base[`@/components/icons/${icon}`] = { [icon]: icon };
}
base['@/components/AppHeader'] = load('components/AppHeader.tsx', {
  ...base,
  './ChamaSequencia': { ChamaSequencia: 'ChamaSequencia' },
});
for (const component of [
  'Card',
  'PrimaryButton',
  'SearchInput',
  'SegmentedTabs',
  'RankingRow',
  'CampoTexto',
]) {
  base[`@/components/${component}`] = load(`components/${component}.tsx`, base);
}
const { RankingRow } = base['@/components/RankingRow'];
const { AppHeader } = base['@/components/AppHeader'];
const { SearchInput } = base['@/components/SearchInput'];
const { SegmentedTabs } = base['@/components/SegmentedTabs'];
const { Card } = base['@/components/Card'];
const { CampoTexto } = base['@/components/CampoTexto'];
const { PrimaryButton } = base['@/components/PrimaryButton'];
const rankingComponents = load('features/clube/RankingCard.tsx', base);
const { RankingCard } = rankingComponents;
const style = (value) =>
  Object.assign({}, ...[value].flat(Infinity).filter(Boolean));
const text = (node) =>
  React.Children.toArray(node.props.children)
    .filter((x) => typeof x === 'string' || typeof x === 'number')
    .join('');
async function mount(component, props = {}) {
  let tree;
  await act(async () => {
    tree = create(React.createElement(component, props));
  });
  return tree;
}
const close = (tree) => act(async () => tree.unmount());
function assertTouchTarget(node) {
  const layout = style(node.props.style);
  assert.ok(layout.minHeight >= tokens.sizes.buttonHeight);
  assert.ok(layout.minWidth >= tokens.sizes.buttonHeight);
}
const row = (id, posicao, pontos = 0) => ({
  posicao,
  pontos,
  usuario: { id, nome: 'Nome completo do participante ' + id, avatarUrl: null },
});

for (const positions of [
  [1, 1, 3],
  [1, 2, 2, 4],
  [1, 1, 1],
  [49, 49, 53],
]) {
  test(`ranking: preserva posições ${positions}, rótulos e alternância visual`, async () => {
    let presses = 0,
      all = 0;
    const items = positions.map((position, index) =>
      row(String(index), position)
    );
    const tree = await mount(RankingCard, {
      ranking: items,
      onLinhaPress: () => presses++,
      onVerTodosPress: () => all++,
    });
    try {
      const rows = tree.root.findAllByType(RankingRow);
      assert.deepEqual(
        rows.map((n) => n.props.position),
        positions
      );
      rows.forEach((n, index) => {
        const accessible = n
          .findAllByType('View')
          .find((v) => v.props.accessible);
        assert.equal(
          accessible.props.accessibilityLabel,
          `${positions[index]}º lugar: ${items[index].usuario.nome}, 0 pontos`
        );
        const stripe = n
          .findAllByType('View')
          .find((v) => v.props.style?.backgroundColor !== undefined);
        assert.equal(
          stripe.props.style.backgroundColor,
          index % 2 ? tokens.colors.surfaceAlt : 'transparent'
        );
      });
      const firstAction = tree.root
        .findAllByType('Pressable')
        .find((n) => n.props.onPress !== undefined);
      act(() => firstAction.props.onPress());
      assert.equal(presses, 1);
      assert.equal(firstAction.props.accessibilityRole, 'button');
      assert.equal(
        firstAction.props.accessibilityLabel,
        `${positions[0]}º lugar: ${items[0].usuario.nome}, 0 pontos`
      );
      const action = tree.root
        .findAllByType('Pressable')
        .find((n) => n.props.accessibilityLabel === 'Ver todos');
      assertTouchTarget(action);
      act(() => action.props.onPress());
      assert.equal(all, 1);
    } finally {
      await close(tree);
    }
  });
}

for (const position of [1, 2, 3, 4]) {
  test(`ranking: nome completo pode ocupar várias linhas na posição ${position}`, async () => {
    const name = 'Lucas Martins de Almeida';
    const tree = await mount(RankingRow, { position, name, points: 1234 });
    try {
      const label = tree.root
        .findAllByType('Text')
        .find((n) => text(n).includes(name));
      assert.equal(label.props.numberOfLines, undefined);
      assert.equal(label.props.ellipsizeMode, undefined);
      assert.equal(text(label), `${position}º lugar: ${name}`);
      const points = tree.root
        .findAllByType('Text')
        .find((n) => text(n) === '1234 pontos');
      if (position < 4) {
        const mask = tree.root.findByType('MaskedView');
        assert.equal(mask.props.maskElement.props.numberOfLines, undefined);
        assert.equal(style(mask.props.style).alignSelf, 'stretch');
        assert.deepEqual(
          tree.root.findByType('LinearGradient').props.colors,
          tokens.gradients[
            ['rankGold', 'rankSilver', 'rankBronze'][position - 1]
          ]
        );
        assert.ok(mask.parent.parent === points.parent);
      } else assert.ok(label.parent === points.parent);
      // Nomes e pontos compartilham uma coluna flexível; não disputam a mesma linha.
      const column = style(points.parent.props.style);
      assert.equal(column.flex, 1);
      assert.equal(column.minWidth, 0);
      assert.notEqual(column.flexDirection, 'row');
    } finally {
      await close(tree);
    }
  });
}

test('ranking completo e paginação mantêm as posições vindas da API', async () => {
  let loadMore = 0;
  const items = [row('a', 49), row('b', 49), row('c', 53)];
  const { RankingClubeScreen } = load('features/clube/RankingClubeScreen.tsx', {
    ...base,
    './RankingCard': rankingComponents,
    './useRankingClube': {
      useRankingClube: () => ({
        ranking: {
          situacao: 'sucesso',
          dados: { clube: { nome: 'Clube' }, ranking: items },
          carregandoMais: false,
        },
        carregarMais: () => loadMore++,
      }),
    },
  });
  const tree = await mount(RankingClubeScreen, { clubeId: 'clube' });
  try {
    assert.deepEqual(
      tree.root.findAllByType(RankingRow).map((n) => n.props.position),
      [49, 49, 53]
    );
    const list = tree.root.findByType('FlatList');
    act(() => list.props.onEndReached());
    assert.equal(loadMore, 1);
  } finally {
    await close(tree);
  }

  const requests = [];
  const { useRankingClube } = load('features/clube/useRankingClube.ts', {
    ...base,
    '@/api/client': load('api/client.ts'),
    './api': {
      buscarRankingClube: (id, cursor, signal) =>
        new Promise((resolve) =>
          requests.push({ id, cursor, signal, resolve })
        ),
    },
  });
  let current;
  function Probe() {
    current = useRankingClube('clube');
    return null;
  }
  const hookTree = await mount(Probe);
  try {
    await act(async () =>
      requests[0].resolve({ ranking: [row('a', 49)], proximoCursor: '1' })
    );
    act(() => current.carregarMais());
    assert.equal(requests[1].cursor, '1');
    await act(async () =>
      requests[1].resolve({
        ranking: [row('a', 49), row('b', 49), row('c', 53)],
        proximoCursor: null,
      })
    );
    assert.deepEqual(
      current.ranking.dados.ranking.map((n) => n.posicao),
      [49, 49, 53]
    );
    assert.deepEqual(
      current.ranking.dados.ranking.map((n) => n.usuario.id),
      ['a', 'b', 'c']
    );
  } finally {
    await close(hookTree);
  }
});

test('Busca: placeholder segue a aba; vazio inicial é diferente de nenhum resultado', async () => {
  const { BuscaView } = load('features/busca/BuscaView.tsx', base);
  const { default: BuscarScreen } = load('app/(tabs)/(busca)/buscar.tsx', {
    'expo-router': { router: { push: noop } },
    '@/features/busca/BuscaView': {
      BuscaView,
      ABA_INICIAL: 'Usuários',
      TIPO_POR_ABA: {
        Livros: 'livros',
        Usuários: 'usuarios',
        Comunidades: 'comunidades',
      },
    },
    '@/features/busca/useBusca': {
      useBusca: (termo, tipo) => ({
        busca: termo
          ? { situacao: 'sucesso', dados: { tipo, itens: [] } }
          : { situacao: 'inicial' },
        recarregar: noop,
      }),
    },
  });
  const tree = await mount(BuscarScreen);
  try {
    assert.equal(
      tree.root.findByType('TextInput').props.placeholder,
      'Nome de usuário ou comunidade'
    );
    assert.equal(
      tree.root.findByType('EmptyState').props.message,
      'Busque por leitores, comunidades ou livros'
    );
    for (const aba of ['Livros', 'Comunidades', 'Usuários']) {
      const tab = tree.root
        .findAllByType('Pressable')
        .find(
          (n) =>
            n.props.accessibilityRole === 'tab' &&
            n.props.accessibilityLabel === aba
        );
      act(() => tab.props.onPress());
      assert.equal(
        tree.root.findByType('TextInput').props.placeholder,
        aba === 'Livros'
          ? 'Título ou autor do livro'
          : 'Nome de usuário ou comunidade'
      );
      act(() =>
        tree.root.findByType('TextInput').props.onChangeText('  xyzxyz  ')
      );
      assert.equal(
        tree.root.findByType('EmptyState').props.message,
        'Nenhum resultado para "xyzxyz".'
      );
    }
  } finally {
    await close(tree);
  }
});

test('abas segmentadas: papel, rótulo, seleção e callback sem repetição', async () => {
  const changes = [];
  const tree = await mount(SegmentedTabs, {
    options: ['Livros', 'Usuários', 'Comunidades'],
    initialOption: 'Usuários',
    onChange: (value) => changes.push(value),
  });
  try {
    const tabs = () => tree.root.findAllByType('Pressable');
    assert.ok(tabs().every((n) => n.props.accessibilityRole === 'tab'));
    assert.deepEqual(
      tabs().map((n) => n.props.accessibilityLabel),
      ['Livros', 'Usuários', 'Comunidades']
    );
    assert.deepEqual(
      tabs().map((n) => n.props.accessibilityState.selected),
      [false, true, false]
    );
    act(() => tabs()[0].props.onPress());
    act(() => tabs()[0].props.onPress());
    assert.ok(
      tree.root
        .findAllByType('Text')
        .every((n) => n.props.numberOfLines === undefined)
    );
    assert.deepEqual(changes, ['Livros']);
    assert.deepEqual(
      tabs().map((n) => n.props.accessibilityState.selected),
      [true, false, false]
    );
  } finally {
    await close(tree);
  }
});

test('Feed: saudação inteira com quebra de linha, selo preservado e Ver todos com alvo de botão', async () => {
  let all = 0;
  const { FeedComunidadesView } = load(
    'features/feed/FeedComunidadesView.tsx',
    { ...base, './ClubeCard': { ClubeCard: 'ClubeCard' } }
  );
  const tree = await mount(FeedComunidadesView, {
    feed: {
      situacao: 'sucesso',
      dados: {
        usuario: { nome: 'Daniel Ribeiro de Almeida', sequenciaDias: 100 },
        clubes: [],
        comunidades: [],
      },
    },
    onVerTodosPress: () => all++,
    onSocialPress: noop,
  });
  try {
    const greeting = tree.root
      .findAllByType('Text')
      .find((n) => text(n) === 'Olá, Daniel Ribeiro de Almeida!');
    assert.equal(greeting.props.numberOfLines, 0);
    assert.ok(
      tree.root
        .findAllByType('View')
        .some((n) =>
          n.props.accessibilityLabel?.startsWith('Sequência de 100 dias')
        )
    );
    const action = tree.root
      .findAllByType('Pressable')
      .find((n) => n.props.accessibilityLabel === 'Ver todos');
    assertTouchTarget(action);
    act(() => action.props.onPress());
    assert.equal(all, 1);
  } finally {
    await close(tree);
  }
});

test('Desafios: cabeçalho compacto e meta só nos duelos de páginas', async () => {
  const template = {
    id: 'a',
    descricao: 'Quem lê mais páginas em 7 dias',
    status: 'em_andamento',
    diasRestantes: 3,
    oponente: { username: 'Amigo' },
    progresso: { voce: 1, oponente: 2 },
  };
  const { DesafiosScreen } = load('features/desafios/DesafioScreen.tsx', {
    ...base,
    '@/api/client': load('api/client.ts'),
    './DesafiosSistema': { DesafiosSistema: 'DesafiosSistema' },
    './api': {
      listarTodosOsDesafios: async () => [
        { ...template, tipoMeta: 'paginas', meta: 150 },
        {
          ...template,
          id: 'b',
          tipoMeta: 'livro',
          descricao: 'Livro de teste',
          livro: {},
        },
      ],
    },
  });
  const tree = await mount(DesafiosScreen);
  try {
    const header = tree.root.findByType(AppHeader);
    assert.equal(header.props.compact, true);
    assert.equal(
      header.findAllByType('View')[0].props.style.minHeight,
      undefined
    );
    const metas = tree.root
      .findAllByType('Text')
      .filter((n) => text(n).startsWith('Meta:'));
    assert.equal(metas.length, 1);
    assert.equal(text(metas[0]), 'Meta: 150 páginas');
    const description = tree.root
      .findAllByType('Text')
      .find((n) => text(n) === template.descricao);
    assert.equal(description.props.numberOfLines, undefined);
  } finally {
    await close(tree);
  }
});

test('Desafiar amigo: Livro Específico permite quebra e lupa não encolhe com o campo', async () => {
  const reload = noop;
  const { DesafiarAmigoScreen } = load(
    'features/desafios/DesafiarAmigoScreen.tsx',
    {
      ...base,
      '@/components/Slider': { Slider: 'Slider' },
      './DesafiarAmigoIcons': {
        CloseIcon: 'CloseIcon',
        CrossedSwordsIcon: 'CrossedSwordsIcon',
        PencilIcon: 'PencilIcon',
      },
      './useDesafiarAmigo': {
        useOponentes: () => ({
          estado: { situacao: 'sucesso', oponentes: [] },
          recarregar: reload,
        }),
        useCriarDesafio: () => ({
          estado: { situacao: 'inicial' },
          enviar: noop,
          limparErro: noop,
        }),
      },
    }
  );
  const tree = await mount(DesafiarAmigoScreen);
  try {
    const label = tree.root
      .findAllByType('Text')
      .find((n) => text(n) === 'Livro Específico');
    assert.equal(label.props.numberOfLines, undefined);
    assert.equal(label.props.adjustsFontSizeToFit, undefined);
    const search = tree.root.findByType(SearchInput);
    const icon = search.findByType('SearchIcon');
    assert.equal(style(icon.parent.props.style).flexShrink, 0);
    assert.equal(style(search.findByType('TextInput').props.style).minWidth, 0);
  } finally {
    await close(tree);
  }
});

test('comunidade: Seguir e Em alta preservam callbacks e têm área de botão', async () => {
  let follow = 0,
    toast = 0;
  const { ComunidadeScreen } = load(
    'features/comunidade/ComunidadeScreen.tsx',
    {
      ...base,
      '@/components/PostCard': { PostCard: 'PostCard' },
      '@/components/toast-provider': {
        useToastContext: () => ({ showToast: () => toast++ }),
      },
      '@/features/comentarios/ComentariosSheet': {
        ComentariosSheet: 'ComentariosSheet',
      },
      '@/components/useVoltar': { useVoltar: () => noop },
      './ComunidadeScreen.styles': load(
        'features/comunidade/ComunidadeScreen.styles.ts',
        base
      ),
      './useComunidade': {
        useComunidade: () => ({
          comunidade: {
            situacao: 'sucesso',
            dados: {
              nome: 'Comunidade',
              sigo: false,
              seguidores: 10,
              posts: [],
            },
          },
          alternarSeguimento: async () => {
            follow++;
          },
        }),
      },
    }
  );
  const tree = await mount(ComunidadeScreen, {
    comunidadeId: 'id',
    origem: '/buscar',
  });
  try {
    for (const label of ['Seguir', 'Em alta']) {
      const action = tree.root
        .findAllByType('Pressable')
        .find((n) => n.props.accessibilityLabel === label);
      assert.equal(action.props.accessibilityRole, 'button');
      assertTouchTarget(action);
      await act(async () => action.props.onPress());
    }
    assert.equal(follow, 1);
    assert.equal(toast, 1);
  } finally {
    await close(tree);
  }
});

async function loginHarness(entrarComSenha = async () => ({ token: 'teste' })) {
  const saved = [],
    navigation = [],
    signups = [];
  const { default: EntrarScreen } = load('app/entrar.tsx', {
    ...base,
    'expo-router': {
      useRouter: () => ({ push: (route) => navigation.push(route) }),
      useLocalSearchParams: () => ({}),
    },
    '@/auth/cognitoProtocolo': load('auth/cognitoProtocolo.ts'),
    '@/auth/cognitoApi': { entrarComSenha, entrarComGoogle: async () => null },
    '@/auth/session': { salvarSessao: async (session) => saved.push(session) },
    '@/auth/cadastroPendente': {
      guardarCadastro: (...args) => signups.push(args),
    },
    '@/components/TelaDeAcesso': {
      TelaDeAcesso: ({ children }) =>
        React.createElement('View', null, children),
    },
    '@/components/useVoltar': { useVoltar: () => noop },
  });
  const tree = await mount(EntrarScreen);
  const field = (name) =>
    tree.root.findAllByType(CampoTexto).find((n) => n.props.rotulo === name);
  const change = (name, value) =>
    act(() => field(name).props.onChangeText(value));
  const submit = () =>
    act(async () =>
      tree.root
        .findAllByType(PrimaryButton)
        .find((n) => n.props.label === 'ENTRAR')
        .props.onPress()
    );
  return { tree, field, change, submit, saved, navigation, signups };
}

for (const email of ['', 'sem-arroba', 'pessoa@', 'pessoa@servidor']) {
  test(`login: e-mail inválido ${JSON.stringify(email)} mostra erro no campo sem chamar Cognito`, async () => {
    let calls = 0;
    const h = await loginHarness(async () => {
      calls++;
    });
    try {
      h.change('Email', email);
      h.change('Senha', 'Senha#123');
      await h.submit();
      assert.equal(calls, 0);
      assert.equal(h.field('Email').props.erro, 'Digite um e-mail válido.');
      assert.ok(
        h
          .field('Email')
          .findAllByType('Text')
          .some((n) => text(n) === 'Digite um e-mail válido.')
      );
      h.change('Email', 'pessoa@exemplo.com');
      assert.equal(h.field('Email').props.erro, null);
      await h.submit();
      assert.equal(calls, 1);
    } finally {
      await close(h.tree);
    }
  });
}

test('login: falta de senha é indicada e envio válido preserva sessão e bloqueio de toque duplo', async () => {
  let resolve,
    calls = 0;
  const h = await loginHarness(() => {
    calls++;
    return new Promise((ok) => {
      resolve = ok;
    });
  });
  try {
    h.change('Email', 'pessoa@exemplo.com');
    await h.submit();
    assert.equal(calls, 0);
    assert.equal(h.field('Senha').props.erro, 'Digite sua senha.');
    h.change('Senha', 'Senha#123');
    let first, second;
    const submit = h.tree.root
      .findAllByType(PrimaryButton)
      .find((n) => n.props.label === 'ENTRAR').props.onPress;
    await act(async () => {
      first = submit();
      second = submit();
    });
    assert.equal(calls, 1);
    assert.ok(h.field('Email').props.editable === false);
    await act(async () => {
      resolve({ token: 'sessão' });
      await Promise.all([first, second]);
    });
    assert.deepEqual(h.saved, [{ token: 'sessão' }]);
  } finally {
    await close(h.tree);
  }
});

test('login: erro específico e conta não confirmada mantêm os tratamentos existentes', async () => {
  // O mesmo módulo precisa fornecer o construtor usado pela tela para instanceof.
  const protocol = load('auth/cognitoProtocolo.ts');
  for (const type of ['credenciais', 'naoConfirmado']) {
    const saved = [],
      navigation = [];
    const { default: Screen } = load('app/entrar.tsx', {
      ...base,
      'expo-router': {
        useRouter: () => ({ push: (route) => navigation.push(route) }),
        useLocalSearchParams: () => ({}),
      },
      '@/auth/cognitoProtocolo': protocol,
      '@/auth/cognitoApi': {
        entrarComSenha: async () => {
          throw new protocol.ErroLogin(type);
        },
      },
      '@/auth/session': { salvarSessao: noop },
      '@/auth/cadastroPendente': {
        guardarCadastro: (...args) => saved.push(args),
      },
      '@/components/TelaDeAcesso': {
        TelaDeAcesso: ({ children }) =>
          React.createElement('View', null, children),
      },
      '@/components/useVoltar': { useVoltar: () => noop },
    });
    const tree = await mount(Screen);
    try {
      const field = (label) =>
        tree.root
          .findAllByType(CampoTexto)
          .find((n) => n.props.rotulo === label);
      act(() => field('Email').props.onChangeText('pessoa@exemplo.com'));
      act(() => field('Senha').props.onChangeText('Senha#123'));
      await act(async () =>
        tree.root
          .findAllByType(PrimaryButton)
          .find((n) => n.props.label === 'ENTRAR')
          .props.onPress()
      );
      if (type === 'credenciais') {
        assert.ok(
          tree.root
            .findAllByType('Text')
            .some((n) => text(n) === protocol.MENSAGENS_ERRO_LOGIN.credenciais)
        );
        assert.equal(field('Senha').props.value, '');
      } else {
        assert.deepEqual(navigation, ['/confirmar-email']);
        assert.deepEqual(saved[0], ['pessoa@exemplo.com', 'Senha#123', null]);
      }
    } finally {
      await close(tree);
    }
  }
});

test('chat: contador usa 1.000 e preserva limite e envio', async () => {
  const { ChatClubeScreen } = load('features/chat/ChatClubeScreen.tsx', {
    ...base,
    '@/components/bonecos': { Boneco: 'Boneco', ehBoneco: () => false },
    '@/components/useAlturaTeclado': { useAlturaTeclado: () => 0 },
    './useChatClube': {
      useChatClube: () => ({
        chat: { situacao: 'sucesso', mensagens: [] },
        enviando: false,
        erroEnvio: null,
        enviar: async () => true,
      }),
    },
  });
  const tree = await mount(ChatClubeScreen, { clubeId: 'id' });
  try {
    assert.ok(
      tree.root.findAllByType('Text').some((n) => text(n) === '0/1.000')
    );
    const input = () => tree.root.findByType('TextInput');
    assert.equal(input().props.maxLength, 1000);
    act(() => input().props.onChangeText('x'.repeat(1000)));
    assert.ok(
      tree.root.findAllByType('Text').some((n) => text(n) === '1.000/1.000')
    );
    assert.equal(
      tree.root.findByType('TouchableOpacity').props.disabled,
      false
    );
  } finally {
    await close(tree);
  }
});

test('Desafios: as três falhas usam o card existente, o mesmo estilo e retries independentes', async () => {
  let friends = 0,
    system = 0;
  const deps = {
    ...base,
    '@/components/ProgressBar': { ProgressBar: 'ProgressBar' },
    '@/components/icons/ClockIcon': { ClockIcon: 'ClockIcon' },
    '@/components/icons/TargetIcon': { TargetIcon: 'TargetIcon' },
  };
  const hook = load('features/desafios/useDesafiosSistema.ts', {
    ...deps,
    './sistemaApi': {
      consultarDesafiosSistema: async () => {
        system++;
        throw new Error('offline');
      },
    },
  });
  const sections = load('features/desafios/DesafiosSistema.tsx', {
    ...deps,
    './useDesafiosSistema': hook,
  });
  const { DesafiosScreen } = load('features/desafios/DesafioScreen.tsx', {
    ...deps,
    './DesafiosSistema': sections,
    './api': {
      listarTodosOsDesafios: async () => {
        friends++;
        throw new Error('offline');
      },
    },
  });
  const tree = await mount(DesafiosScreen);
  try {
    const errors = () =>
      tree.root
        .findAllByType('Text')
        .filter((n) => n.props.accessibilityRole === 'alert');
    assert.equal(errors().length, 3);
    for (const error of errors()) {
      assert.equal(text(error), 'Não foi possível carregar. Tente novamente.');
      const layout = style(error.props.style);
      assert.equal(layout.fontSize, theme.textStyles.bodySmall.fontSize);
      assert.equal(layout.color, tokens.colors.danger);
      let ancestor = error.parent;
      while (ancestor && ancestor.type !== Card) ancestor = ancestor.parent;
      assert.ok(ancestor, 'mensagem deve estar no Card existente');
    }
    const retries = () =>
      tree.root
        .findAllByType(PrimaryButton)
        .filter((n) => n.props.label === 'Tentar de novo');
    assert.equal(retries().length, 3);
    await act(async () => retries()[0].props.onPress());
    assert.equal(system, 2);
    assert.equal(friends, 1);
    await act(async () => retries()[1].props.onPress());
    assert.equal(system, 3);
    assert.equal(friends, 1);
    await act(async () => retries()[2].props.onPress());
    assert.equal(system, 3);
    assert.equal(friends, 2);
  } finally {
    await close(tree);
  }
});
