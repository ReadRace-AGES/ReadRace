const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
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

const { ApiError } = load('api/client.ts', {});
const modelo = load('features/feed/atividadeSocial.ts', {});
const nativos = {
  'react-native': {
    ActivityIndicator: 'ActivityIndicator',
    FlatList: 'FlatList',
    Keyboard: { dismiss: () => {} },
    Pressable: 'Pressable',
    ScrollView: 'ScrollView',
    StyleSheet: { create: (styles) => styles },
    Text: 'Text',
    View: 'View',
  },
  'react-native-svg': {
    __esModule: true,
    default: 'Svg',
    Circle: 'Circle',
    Path: 'Path',
  },
  '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
  '@/components/AppHeader': { AppHeader: 'AppHeader' },
  '@/components/avatar': { Avatar: 'Avatar', StreakBadge: 'StreakBadge' },
  '@/components/Card': { Card: 'Card' },
  '@/components/EmptyState': { EmptyState: 'EmptyState' },
  '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
  '@/components/icons/UsersIcon': { UsersIcon: 'UsersIcon' },
  '@/components/ListItem': { ListItem: 'ListItem' },
  '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '@/components/SegmentedTabs': { SegmentedTabs: 'SegmentedTabs' },
  '@/features/livro/model': { tempoRelativo: () => '3h atrás' },
};

// 18/10/2026, 12h em São Paulo.
const AGORA = Date.parse('2026-10-18T15:00:00Z');

function atividade(extra = {}) {
  return {
    tipo: 'leitura',
    usuario: {
      id: 'ana',
      nome: 'Ana',
      avatarUrl: null,
      sequenciaDias: 4,
    },
    livro: {
      id: 'livro-1984',
      titulo: '1984',
      autor: 'George Orwell',
      totalPaginas: 328,
    },
    paginas: 50,
    xp: 50,
    em: '2026-10-18T09:00:00-03:00',
    ...extra,
  };
}

function textos(tree) {
  return tree.root
    .findAllByType('Text')
    .map((node) => node.props.children)
    .flat();
}

test('API chama a rota da aba Social e encaminha o cancelamento', async () => {
  const chamadas = [];
  const { buscarFeedSocial } = load('features/feed/api.ts', {
    '@/api/client': {
      apiGet: async (...args) => {
        chamadas.push(args);
        return { atividades: [] };
      },
    },
  });
  const controller = new AbortController();
  await buscarFeedSocial(controller.signal);
  assert.equal(chamadas[0][0], '/api/feed/social');
  assert.equal(chamadas[0][1].signal, controller.signal);
});

test('título diz "hoje" só no dia de hoje, no fuso que veio do servidor', () => {
  assert.equal(
    modelo.tituloDaAtividade(atividade(), AGORA),
    'Leu 50 páginas hoje!'
  );
  // 23h de ontem em São Paulo, mas já é dia 18 em UTC: não é de hoje.
  assert.equal(
    modelo.tituloDaAtividade(
      atividade({ em: '2026-10-17T23:00:00-03:00' }),
      AGORA
    ),
    'Leu 50 páginas'
  );
  assert.equal(modelo.foiHoje('2026-10-18T00:30:00-03:00', AGORA), true);
  // 22h de São Paulo, já é dia 19 em UTC: continua sendo hoje lá.
  assert.equal(
    modelo.foiHoje(
      '2026-10-18T22:00:00-03:00',
      Date.parse('2026-10-19T01:30:00Z')
    ),
    true
  );
  assert.equal(
    modelo.tituloDaAtividade(atividade({ paginas: 1, xp: 1 }), AGORA),
    'Leu 1 página hoje!'
  );
});

test('linhas do livro e dos números seguem o Figma', () => {
  assert.equal(modelo.linhaDoLivro(atividade()), '1984 - George Orwell');
  assert.equal(
    modelo.linhaDoLivro(
      atividade({ livro: { ...atividade().livro, autor: null } })
    ),
    '1984'
  );
  assert.equal(modelo.linhaDeNumeros(atividade()), '50 páginas • +50 XP');
});

test('conclusão: "Terminou o livro" com o total do livro e o bônus, hoje ou não', () => {
  const conclusao = atividade({ tipo: 'conclusao', paginas: 328, xp: 150 });
  assert.equal(modelo.tituloDaAtividade(conclusao, AGORA), 'Terminou o livro');
  assert.equal(
    modelo.tituloDaAtividade(
      { ...conclusao, em: '2026-10-10T09:00:00-03:00' },
      AGORA
    ),
    'Terminou o livro'
  );
  assert.equal(modelo.linhaDoLivro(conclusao), '1984 - George Orwell');
  assert.equal(modelo.linhaDeNumeros(conclusao), '328 páginas • +150 XP');
});

test('card mostra autor, selo de sequência, tempo, título, livro e números, e abre o perfil e o livro', () => {
  const { AtividadeCard } = load('features/feed/AtividadeCard.tsx', {
    ...nativos,
    './atividadeSocial': modelo,
  });
  const realNow = Date.now;
  Date.now = () => AGORA;
  const toques = [];
  let tree;
  try {
    act(() => {
      tree = create(
        React.createElement(AtividadeCard, {
          atividade: atividade(),
          onAutorPress: () => toques.push('autor'),
          onLivroPress: () => toques.push('livro'),
        })
      );
    });
  } finally {
    Date.now = realNow;
  }
  assert.equal(tree.root.findByType('Avatar').props.name, 'Ana');
  assert.equal(tree.root.findByType('StreakBadge').props.value, 4);
  assert.deepEqual(
    textos(tree).filter((t) => typeof t === 'string'),
    [
      'Ana',
      '3h atrás',
      'Leu 50 páginas hoje!',
      '1984 - George Orwell',
      '50 páginas • +50 XP',
    ]
  );
  // Só dois toques: autor e livro. Sem curtir nem comentar.
  const [autor, livro] = tree.root.findAllByType('Pressable');
  assert.equal(tree.root.findAllByType('Pressable').length, 2);
  assert.ok(autor.findByType('Avatar'));
  assert.equal(autor.props.accessibilityLabel, 'Abrir o perfil de Ana');
  assert.equal(livro.props.accessibilityLabel, 'Abrir o livro 1984');
  autor.props.onPress();
  livro.props.onPress();
  assert.deepEqual(toques, ['autor', 'livro']);
  act(() => tree.unmount());
});

test('corpo da aba Social: carregando, erro com tentar de novo, vazio com BUSCAR LEITORES e lista', () => {
  const { FeedSocialView } = load('features/feed/FeedSocialView.tsx', {
    ...nativos,
    './AtividadeCard': { AtividadeCard: 'AtividadeCard' },
  });
  let recargas = 0;
  let buscas = 0;
  const abertos = [];
  const props = (social) => ({
    social,
    recarregar: () => recargas++,
    onBuscarLeitoresPress: () => buscas++,
    onUsuarioPress: (id) => abertos.push(['usuario', id]),
    onLivroPress: (id) => abertos.push(['livro', id]),
  });
  let tree;
  act(() => {
    tree = create(
      React.createElement(FeedSocialView, props({ situacao: 'carregando' }))
    );
  });
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);

  act(() =>
    tree.update(
      React.createElement(
        FeedSocialView,
        props({ situacao: 'erro', mensagem: 'Falhou.' })
      )
    )
  );
  const erro = tree.root.findByType('EmptyState');
  assert.equal(erro.props.message, 'Falhou.');
  erro.props.action.props.onPress();
  assert.equal(recargas, 1);

  act(() =>
    tree.update(
      React.createElement(
        FeedSocialView,
        props({ situacao: 'sucesso', dados: { atividades: [] } })
      )
    )
  );
  const vazio = tree.root.findByType('EmptyState');
  assert.equal(
    vazio.props.message,
    'Siga outros leitores para ver o que eles estão lendo.'
  );
  assert.equal(vazio.props.action.props.label, 'BUSCAR LEITORES');
  vazio.props.action.props.onPress();
  assert.equal(buscas, 1);

  // A leitura e a conclusão do mesmo dia têm o mesmo horário: a chave não pode repetir.
  const lista = [
    atividade({ tipo: 'conclusao', paginas: 328, xp: 150 }),
    atividade(),
    atividade({ usuario: { ...atividade().usuario, id: 'bruno' } }),
  ];
  act(() =>
    tree.update(
      React.createElement(
        FeedSocialView,
        props({ situacao: 'sucesso', dados: { atividades: lista } })
      )
    )
  );
  const flatList = tree.root.findByType('FlatList');
  assert.deepEqual(flatList.props.data, lista);
  const chaves = lista.map((item) => flatList.props.keyExtractor(item));
  assert.equal(new Set(chaves).size, 3);
  const card = flatList.props.renderItem({ item: lista[2] });
  card.props.onAutorPress();
  card.props.onLivroPress();
  assert.deepEqual(abertos, [
    ['usuario', 'bruno'],
    ['livro', 'livro-1984'],
  ]);
  act(() => tree.unmount());
});

test('Feed: Social fica selecionada, busca só quando aberta, tenta de novo e leva à Busca, ao perfil e ao livro', async () => {
  const requests = [];
  const api = load('features/feed/api.ts', {
    '@/api/client': {
      apiGet: (url, { signal }) =>
        new Promise((resolve, reject) =>
          requests.push({ url, signal, resolve, reject })
        ),
    },
  });
  const useFeedComunidades = load('features/feed/useFeedComunidades.ts', {
    '@/api/client': { ApiError },
    './api': api,
  });
  const useFeedSocial = load('features/feed/useFeedSocial.ts', {
    '@/api/client': { ApiError },
    './api': api,
  });
  const feedView = load('features/feed/FeedComunidadesView.tsx', {
    ...nativos,
    './ClubeCard': { ClubeCard: 'ClubeCard' },
    './FeedSocialView': { FeedSocialView: 'FeedSocialView' },
  });
  const navegacoes = [];
  const empilhadas = [];
  let toasts = 0;
  const { default: FeedScreen } = load('app/(tabs)/(feed)/feed.tsx', {
    'expo-router': {
      router: {
        push: (rota) => empilhadas.push(rota),
        navigate: (rota) => navegacoes.push(rota),
      },
      useFocusEffect: (callback) => React.useEffect(callback, [callback]),
    },
    'react-native': nativos['react-native'],
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => toasts++ }),
    },
    '@/features/feed/FeedComunidadesView': feedView,
    '@/features/feed/CriarGrupoSheet': { CriarGrupoSheet: 'CriarGrupoSheet' },
    '@/features/feed/useFeedComunidades': useFeedComunidades,
    '@/features/feed/useFeedSocial': useFeedSocial,
  });
  let tree;
  await act(async () => {
    tree = create(React.createElement(FeedScreen));
  });
  const view = () => tree.root.findByType(feedView.FeedComunidadesView);
  const social = () => tree.root.findByType('FeedSocialView');
  assert.equal(view().props.aba, 'Comunidades');
  assert.deepEqual(
    requests.map((r) => r.url),
    ['/api/feed/comunidades']
  );
  await act(async () =>
    requests[0].resolve({
      usuario: { nome: 'Eu', sequenciaDias: 1 },
      clubes: [],
      comunidades: [],
    })
  );
  assert.equal(tree.root.findAllByType('FeedSocialView').length, 0);

  await act(async () =>
    tree.root.findByType('SegmentedTabs').props.onChange('Social')
  );
  assert.equal(view().props.aba, 'Social');
  assert.equal(tree.root.findByType('SegmentedTabs').props.value, 'Social');
  assert.equal(toasts, 0);
  assert.equal(social().props.social.situacao, 'carregando');
  assert.deepEqual(
    requests.map((r) => r.url),
    ['/api/feed/comunidades', '/api/feed/social']
  );
  // A aba Comunidades some enquanto Social está selecionada.
  assert.equal(tree.root.findAllByType('ScrollView').length, 0);

  await act(async () =>
    requests[1].reject(new ApiError(500, { message: 'Falhou.' }))
  );
  assert.deepEqual(social().props.social, {
    situacao: 'erro',
    mensagem: 'Falhou.',
  });
  await act(async () => social().props.recarregar());
  assert.equal(social().props.social.situacao, 'carregando');
  assert.equal(requests.length, 3);
  await act(async () => requests[2].resolve({ atividades: [] }));
  assert.deepEqual(social().props.social.dados, { atividades: [] });

  act(() => social().props.onBuscarLeitoresPress());
  assert.equal(navegacoes.length, 1);
  assert.equal(navegacoes[0].pathname, '/buscar');
  assert.equal(navegacoes[0].params.aba, 'Usuários');
  assert.ok(navegacoes[0].params.pedidoEm);

  // Perfil e livro empilham dentro da própria aba Feed: o voltar traz à aba Social.
  act(() => social().props.onUsuarioPress('ana'));
  act(() => social().props.onLivroPress('livro-1984'));
  assert.deepEqual(empilhadas, [
    {
      pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
      params: { usuarioId: 'ana' },
    },
    {
      pathname: '/(tabs)/(feed)/feed-livro/[livroId]',
      params: { livroId: 'livro-1984' },
    },
  ]);
  assert.equal(view().props.aba, 'Social');

  await act(async () =>
    tree.root.findByType('SegmentedTabs').props.onChange('Comunidades')
  );
  assert.equal(tree.root.findAllByType('FeedSocialView').length, 0);
  assert.equal(requests.length, 3);
  act(() => tree.unmount());
});

test('Busca troca para a aba pedida por parâmetro, mesmo repetindo o pedido', async () => {
  let params = {};
  const busca = load('features/busca/BuscaView.tsx', {
    ...nativos,
    '@/components/LinhaUsuario': { LinhaUsuario: 'LinhaUsuario' },
    '@/components/BookCover': { BookCover: 'BookCover' },
    '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
    '@/components/SearchInput': { SearchInput: 'SearchInput' },
  });
  const { default: BuscarScreen } = load('app/(tabs)/(busca)/buscar.tsx', {
    'expo-router': {
      router: { push: () => {} },
      useLocalSearchParams: () => params,
    },
    '@/features/busca/BuscaView': { ...busca, BuscaView: 'BuscaView' },
    '@/features/busca/useBusca': {
      useBusca: () => ({ busca: { situacao: 'inicial' }, recarregar() {} }),
    },
  });
  let tree;
  await act(async () => {
    tree = create(React.createElement(BuscarScreen));
  });
  const view = () => tree.root.findByType('BuscaView');
  assert.equal(view().props.aba, 'Usuários');
  act(() => view().props.onAbaChange('Livros'));
  assert.equal(view().props.aba, 'Livros');

  params = { aba: 'Usuários', pedidoEm: '1' };
  await act(async () => tree.update(React.createElement(BuscarScreen)));
  assert.equal(view().props.aba, 'Usuários');

  act(() => view().props.onAbaChange('Comunidades'));
  params = { aba: 'Usuários', pedidoEm: '2' };
  await act(async () => tree.update(React.createElement(BuscarScreen)));
  assert.equal(view().props.aba, 'Usuários');

  params = { aba: 'Inexistente', pedidoEm: '3' };
  await act(async () => tree.update(React.createElement(BuscarScreen)));
  assert.equal(view().props.aba, 'Usuários');
  act(() => tree.unmount());
});

test('rotas do Feed abrem o perfil e o livro com a origem da aba Feed', () => {
  const telas = {
    '@/features/perfil/PerfilScreen': { PerfilScreen: 'PerfilScreen' },
    '@/features/livro/LivroDetalheScreen': {
      LivroDetalheScreen: 'LivroDetalheScreen',
    },
  };
  const base = 'app/(tabs)/(feed)';
  const { default: Perfil } = load(`${base}/perfil-usuario/[usuarioId].tsx`, {
    ...telas,
    'expo-router': { useLocalSearchParams: () => ({ usuarioId: 'ana' }) },
  });
  assert.equal(Perfil().type, 'PerfilScreen');
  assert.equal(Perfil().props.usuarioId, 'ana');
  assert.equal(Perfil().props.origem, 'feed');

  const { default: Livro } = load(`${base}/feed-livro/[livroId].tsx`, {
    ...telas,
    'expo-router': { useLocalSearchParams: () => ({ livroId: 'livro-1984' }) },
  });
  assert.equal(Livro().type, 'LivroDetalheScreen');
  assert.equal(Livro().props.livroId, 'livro-1984');
  assert.equal(Livro().props.origem, '/feed');
});
