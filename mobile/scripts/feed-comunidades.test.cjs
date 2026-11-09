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
const native = Object.fromEntries(
  ['ActivityIndicator', 'Text', 'View', 'ScrollView'].map((n) => [n, n])
);
native.Pressable = ({ children, ...props }) =>
  React.createElement(
    'Pressable',
    props,
    typeof children === 'function' ? children({ pressed: false }) : children
  );
native.StyleSheet = { create: (s) => s };
native.Keyboard = { dismiss() {} };
const deps = {
  'react-native': native,
  'react-native-svg': {
    __esModule: true,
    default: 'Svg',
    Circle: 'Circle',
    Path: 'Path',
  },
  '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
};
for (const name of [
  'AppHeader',
  'EmptyState',
  'PrimaryButton',
  'SegmentedTabs',
  'Card',
  'BookCover',
])
  deps[`@/components/${name}`] = { [name]: name };
deps['@/components/avatar'] = { Avatar: 'Avatar' };
deps['@/components/icons/BookIcon'] = { BookIcon: 'BookIcon' };
const list = load('components/ListItem.tsx', deps);
const view = load('features/feed/FeedComunidadesView.tsx', {
  ...deps,
  '@/components/ListItem': list,
  './ClubeCard': { ClubeCard: 'ClubeCard' },
  './FeedSocialView': { FeedSocialView: 'FeedSocialView' },
});
const suggestion = (id) => ({
  id,
  nome: `Sugestão ${id}`,
  imagemUrl: `imagem-${id}`,
  membros: 12,
});
const member = (id) => ({
  id,
  nome: `Minha ${id}`,
  capaUrl: `capa-${id}`,
  totalMembros: 7,
});
const data = (
  comunidades = [],
  comunidadesSugeridas = [suggestion('a'), suggestion('b')]
) => ({
  usuario: { nome: 'Ana' },
  clubes: [{ id: 'clube' }],
  comunidades,
  comunidadesSugeridas,
});
async function mount(initial = data()) {
  const h = {
    gets: [],
    posts: [],
    success: [],
    errors: [],
    routes: [],
    focus: [],
  };
  function request(list, url, options) {
    return new Promise((resolve, reject) =>
      list.push({ url, ...options, resolve, reject })
    );
  }
  const client = {
    apiGet: (url, options) => request(h.gets, url, options),
    apiRequest: (url, options) => request(h.posts, url, options),
    mensagemErroCarregamento: () => 'Falha ao carregar.',
  };
  const api = load('features/feed/api.ts', { '@/api/client': client });
  const communityApi = load('features/comunidade/api.ts', {
    '@/api/client': client,
  });
  const hook = load('features/feed/useFeedComunidades.ts', {
    '@/api/client': client,
    './api': api,
    '@/features/comunidade/api': communityApi,
  });
  const atualizarSocial = () => {};
  const { default: Screen } = load('app/(tabs)/(feed)/feed.tsx', {
    'react-native': native,
    'expo-router': {
      router: {
        push: (r) => h.routes.push(r),
        navigate: (r) => h.routes.push(r),
      },
      useFocusEffect(callback) {
        const index = React.useRef(h.focus.length);
        h.focus[index.current] = callback;
        React.useEffect(callback, [callback]);
      },
    },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showSuccessToast: (lines) => h.success.push(lines),
        showErrorToast: (text) => h.errors.push(text),
      }),
    },
    '@/features/feed/FeedComunidadesView': view,
    '@/features/feed/CriarGrupoSheet': { CriarGrupoSheet: 'CriarGrupoSheet' },
    '@/features/feed/useFeedComunidades': {
      useFeedComunidades() {
        h.state = hook.useFeedComunidades();
        return h.state;
      },
    },
    '@/features/feed/useFeedSocial': {
      useFeedSocial: () => ({ atualizar: atualizarSocial }),
    },
  });
  await act(async () => {
    h.tree = create(React.createElement(Screen));
  });
  await act(async () => h.gets[0].resolve(initial));
  h.rows = () => h.tree.root.findAllByType(list.ListItem);
  h.join = (id) =>
    h
      .rows()
      .find((n) => n.props.title === suggestion(id).nome)
      .props.action.onPress();
  h.texts = () =>
    h.tree.root.findAllByType('Text').map((n) => n.props.children);
  h.dispose = () => act(async () => h.tree.unmount());
  h.focusFeed = () => act(async () => h.focus.forEach((fn) => fn()));
  return h;
}
test('Feed mapeia listas e campos distintos, mantém clubes e navegação', async () => {
  const h = await mount(data([member('m')]));
  try {
    assert.equal(h.gets[0].url, '/api/feed/comunidades');
    assert.ok(h.gets[0].signal instanceof AbortSignal);
    const [mine, suggested] = h.rows();
    assert.deepEqual(
      [
        mine.props.title,
        mine.props.imageUrl,
        mine.props.memberCount,
        mine.props.action,
      ],
      ['Minha m', 'capa-m', 7, undefined]
    );
    assert.deepEqual(
      [
        suggested.props.title,
        suggested.props.imageUrl,
        suggested.props.memberCount,
        suggested.props.action.label,
      ],
      ['Sugestão a', 'imagem-a', 12, 'Participar']
    );
    act(() => {
      mine.props.onPress();
      suggested.props.onPress();
    });
    assert.deepEqual(h.routes, [
      '/(tabs)/(feed)/comunidade/m',
      '/(tabs)/(feed)/comunidade/a',
    ]);
    act(() => h.tree.root.findByType('ClubeCard').props.onPress());
    assert.equal(h.routes.at(-1), '/clube/clube');
    const screen = h.tree.root.findByType(view.FeedComunidadesView);
    act(() => screen.props.onVerTodosPress());
    assert.equal(h.routes.at(-1), '/clubes');
    act(() => screen.props.onCriarGrupoPress());
    assert.equal(h.tree.root.findByType('CriarGrupoSheet').props.visible, true);
  } finally {
    await h.dispose();
  }
});
test('vazio exato de Minhas comunidades é independente das sugestões; sugestões vazias ocultam seção', async () => {
  const h = await mount();
  try {
    assert.equal(
      h
        .texts()
        .filter((t) => t === 'Você ainda não participa de nenhuma comunidade.')
        .length,
      1
    );
    assert.ok(h.texts().includes('Minhas comunidades'));
    assert.ok(h.texts().includes('Comunidades sugeridas'));
    assert.equal(h.rows().length, 2);
    await h.focusFeed();
    await act(async () => h.gets[1].resolve(data([], [])));
    assert.equal(h.texts().includes('Comunidades sugeridas'), false);
    assert.equal(h.rows().length, 0);
    assert.equal(
      h
        .texts()
        .filter((t) => t === 'Você ainda não participa de nenhuma comunidade.')
        .length,
      1
    );
  } finally {
    await h.dispose();
  }
});
test('Participar: loading, bloqueio síncrono e acessível, ação sem navegação, sucesso sem duplicar e contagem do servidor', async () => {
  const h = await mount(data([], [suggestion('a')]));
  try {
    await act(async () => {
      const join = h.tree.root
        .findAllByType('Pressable')
        .find(
          (node) => node.props.accessibilityLabel === 'Participar: Sugestão a'
        ).props.onPress;
      join();
      join();
    });
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].url, '/api/comunidades/a/membros/eu');
    assert.equal(h.posts[0].method, 'POST');
    const controls = h.tree.root.findAllByType(native.Pressable);
    const action = controls.find(
      (n) => n.props.accessibilityLabel === 'Participar: Sugestão a'
    );
    const row = controls.find(
      (n) => n.props.accessibilityLabel === 'Sugestão a, 12 membros'
    );
    assert.equal(action.parent, row.parent);
    assert.equal(action.props.disabled, true);
    assert.deepEqual(action.props.accessibilityState, {
      disabled: true,
      busy: true,
    });
    assert.equal(h.tree.root.findAllByType('ActivityIndicator').length, 1);
    act(() => action.props.onPress());
    assert.equal(h.posts.length, 1);
    assert.deepEqual(h.routes, []);
    await h.focusFeed();
    await act(async () => h.gets[1].resolve(data([member('a')], [])));
    await act(async () => h.posts[0].resolve({ sigo: true, seguidores: 43 }));
    assert.deepEqual(h.state.feed.dados.comunidades, [
      { id: 'a', nome: 'Sugestão a', capaUrl: 'imagem-a', totalMembros: 43 },
    ]);
    assert.deepEqual(h.state.feed.dados.comunidadesSugeridas, []);
    assert.equal(h.rows()[0].props.memberCount, 43);
    assert.equal(h.texts().includes('Comunidades sugeridas'), false);
    assert.deepEqual(h.success, [['Você agora segue esta comunidade.']]);
    assert.deepEqual(h.errors, []);
  } finally {
    await h.dispose();
  }
});
test('falha mantém sugestão, libera retry e avisa exatamente; nova tentativa funciona', async () => {
  const h = await mount();
  try {
    await act(async () => {
      h.join('a');
    });
    await act(async () => h.posts[0].reject(new Error('Falha')));
    assert.deepEqual(h.errors, [
      'Não foi possível participar. Tente novamente.',
    ]);
    assert.equal(h.rows()[0].props.action.loading, false);
    assert.equal(h.state.participacoesPendentes.size, 0);
    assert.equal(h.state.feed.dados.comunidadesSugeridas.length, 2);
    await act(async () => {
      h.join('a');
    });
    assert.equal(h.posts.length, 2);
    await act(async () => h.posts[1].resolve({ sigo: true, seguidores: 20 }));
    assert.equal(h.state.feed.dados.comunidades[0].totalMembros, 20);
    assert.deepEqual(h.success, [['Você agora segue esta comunidade.']]);
  } finally {
    await h.dispose();
  }
});
test('participações simultâneas preservam atualizações e GET antigo não desfaz sucesso; novo foco reconcilia perfil', async () => {
  const h = await mount();
  try {
    await h.focusFeed();
    await act(async () => {
      h.join('a');
      h.join('b');
    });
    assert.equal(h.state.participacoesPendentes.size, 2);
    await act(async () => {
      h.posts[1].resolve({ sigo: true, seguidores: 99 });
      h.posts[0].resolve({ sigo: true, seguidores: 42 });
    });
    await act(async () => h.gets[1].resolve(data()));
    assert.deepEqual(
      h.state.feed.dados.comunidades.map((c) => [c.id, c.totalMembros]).sort(),
      [
        ['a', 42],
        ['b', 99],
      ]
    );
    assert.deepEqual(h.state.feed.dados.comunidadesSugeridas, []);
    assert.equal(h.state.participacoesPendentes.size, 0);
    assert.equal(h.success.length, 2);
    await h.focusFeed();
    await act(async () =>
      h.gets[2].resolve(
        data([member('b'), member('perfil')], [suggestion('a')])
      )
    );
    assert.deepEqual(
      h.state.feed.dados.comunidades.map((c) => c.id),
      ['b', 'perfil']
    );
    assert.deepEqual(
      h.state.feed.dados.comunidadesSugeridas.map((c) => c.id),
      ['a']
    );
  } finally {
    await h.dispose();
  }
});
test('falha de recarga posterior preserva POST concluído sem aviso de participação falha', async () => {
  const h = await mount();
  try {
    await act(async () => {
      h.join('a');
    });
    await act(async () => h.posts[0].resolve({ sigo: true, seguidores: 13 }));
    await h.focusFeed();
    await act(async () => h.gets[1].reject(new Error('GET falhou')));
    assert.equal(h.state.feed.situacao, 'sucesso');
    assert.equal(h.state.feed.dados.comunidades[0].id, 'a');
    assert.deepEqual(h.success, [['Você agora segue esta comunidade.']]);
    assert.deepEqual(h.errors, []);
  } finally {
    await h.dispose();
  }
});
