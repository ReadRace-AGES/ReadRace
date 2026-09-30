const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, dependencies) {
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

const { normalizarTermoOponente } = load('features/desafios/api.ts', {
  '@/api/client': {},
});

test('busca reconcilia seleção e bloqueia envio durante loading e erro', async () => {
  const requests = [];
  const posts = [];
  const routes = [];
  let anterior = '/desafios';
  const api = {
    normalizarTermoOponente,
    buscarOponentes: (termo, signal) =>
      new Promise((resolve, reject) =>
        requests.push({ termo, signal, resolve, reject })
      ),
    criarDesafio: async (body) => {
      posts.push(body);
      return {};
    },
  };
  const hooks = load('features/desafios/useDesafiarAmigo.ts', {
    '@/api/client': { ApiError: class ApiError extends Error {} },
    './api': api,
  });
  const dependencies = {
    'expo-router': {
      useLocalSearchParams: () => ({}),
      useRouter: () => ({
        canGoBack: () => anterior !== null,
        back: () => assert.fail('back pode voltar para outra tela'),
        replace: () => assert.fail('replace pode duplicar as abas'),
        dismissTo: (route) => routes.push(route),
      }),
    },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      KeyboardAvoidingView: 'KeyboardAvoidingView',
      Platform: { OS: 'web' },
      Pressable: 'Pressable',
      ScrollView: 'ScrollView',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      TextInput: 'TextInput',
      View: 'View',
    },
    'react-native-safe-area-context': {
      useSafeAreaInsets: () => ({ bottom: 0 }),
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/BookCover': { BookCover: 'BookCover' },
    '@/components/Card': { Card: 'Card' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/SearchInput': { SearchInput: 'SearchInput' },
    '@/components/Slider': { Slider: 'Slider' },
    './DesafiarAmigoIcons': {
      CrossedSwordsIcon: 'CrossedSwordsIcon',
      PencilIcon: 'PencilIcon',
    },
    './useDesafiarAmigo': hooks,
  };
  const { DesafiarAmigoScreen } = load(
    'features/desafios/DesafiarAmigoScreen.tsx',
    dependencies
  );
  let tree;
  const amigo = { id: '1', username: 'Aninha07', avatarUrl: null };
  const outro = { id: '2', username: 'Maria_L', avatarUrl: null };
  const button = () =>
    tree.root
      .findAllByType('PrimaryButton')
      .find((node) => node.props.label === 'Enviar Desafio');
  const hint = () =>
    tree.root
      .findAllByType('Text')
      .filter((node) => node.props.children === 'Escolha um oponente');
  const search = (termo) =>
    act(() => tree.root.findByType('SearchInput').props.onChangeText(termo));
  const receive = async (index, oponentes) => {
    await act(async () => requests[index].resolve({ oponentes }));
  };
  const waitForSearch = async () => {
    await act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  };

  await act(async () => {
    tree = create(React.createElement(DesafiarAmigoScreen));
  });
  await waitForSearch();
  assert.equal(requests[0].termo, '');
  await receive(0, [amigo, outro]);
  assert.equal(button().props.disabled, true);
  assert.equal(hint().length, 1);
  assert.equal(hint()[0].props.accessibilityLiveRegion, 'polite');
  await act(async () => button().props.onPress());
  assert.equal(posts.length, 0);
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.deepEqual(routes, ['/desafios']);
  for (const origem of [null, '/perfil']) {
    anterior = origem;
    act(() => tree.root.findByType('AppHeader').props.onBackPress());
    assert.equal(routes.at(-1), '/desafios');
  }
  routes.splice(1);

  act(() => tree.root.findAllByType('Pressable')[0].props.onPress());
  assert.equal(button().props.disabled, false);
  assert.equal(hint().length, 0);

  search('@ana');
  assert.equal(tree.root.findByType('SearchInput').props.value, '@ana');
  assert.equal(button().props.disabled, true);
  await waitForSearch();
  assert.equal(requests[1].termo, 'ana');
  await receive(1, [amigo]);
  assert.equal(button().props.disabled, false);

  act(() => tree.root.findAllByType('Card')[1].props.onPress());
  assert.equal(button().props.disabled, true);
  act(() => tree.root.findAllByType('Card')[0].props.onPress());
  assert.equal(button().props.disabled, false);

  search('Maria');
  assert.equal(button().props.disabled, true);
  await waitForSearch();
  await receive(2, [outro]);
  assert.equal(button().props.disabled, true);
  assert.equal(hint().length, 1);

  search('');
  assert.equal(button().props.disabled, true);
  await waitForSearch();
  await receive(3, [amigo, outro]);
  assert.equal(button().props.disabled, true);
  assert.equal(hint().length, 1);

  act(() => tree.root.findAllByType('Pressable')[1].props.onPress());
  assert.equal(button().props.disabled, false);
  search('falha');
  assert.equal(button().props.disabled, true);
  await waitForSearch();
  await act(async () => requests[4].reject(new Error('offline')));
  assert.equal(button().props.disabled, true);
  search('');
  await waitForSearch();
  await receive(5, [amigo, outro]);
  assert.equal(button().props.disabled, false);

  await act(async () => button().props.onPress());
  assert.equal(posts.length, 1);
  assert.equal(posts[0].prazoDias, 7);
  const prazo = () =>
    tree.root
      .findAllByType('Pressable')
      .find((node) =>
        node.props.accessibilityLabel?.startsWith('Editar prazo.')
      );
  assert.equal(
    prazo().props.accessibilityLabel,
    'Editar prazo. Em 7 dias corridos'
  );
  act(() => prazo().props.onPress());
  for (const invalido of ['0', '-1', '1.5', '2147483648', '']) {
    act(() => tree.root.findByType('TextInput').props.onChangeText(invalido));
    assert.equal(button().props.disabled, true);
  }
  act(() => tree.root.findByType('TextInput').props.onChangeText('1'));
  assert.equal(button().props.disabled, false);
  act(() => tree.root.findByType('TextInput').props.onBlur());
  assert.equal(
    prazo().props.accessibilityLabel,
    'Editar prazo. Em 1 dia corrido'
  );
  await act(async () => button().props.onPress());
  assert.equal(posts[1].prazoDias, 1);
  assert.equal(posts[0].oponenteId, outro.id);
  assert.deepEqual(routes, ['/desafios', '/desafios', '/desafios']);
  act(() => tree.unmount());
});

test('resposta antiga e debounce não tornam a lista anterior válida', async () => {
  const requests = [];
  const { useOponentes } = load('features/desafios/useDesafiarAmigo.ts', {
    '@/api/client': { ApiError: class ApiError extends Error {} },
    './api': {
      normalizarTermoOponente,
      buscarOponentes: (termo, signal) =>
        new Promise((resolve) => requests.push({ termo, signal, resolve })),
    },
  });
  let estado;
  function Busca({ termo }) {
    ({ estado } = useOponentes(termo));
    return React.createElement('View');
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Busca, { termo: '' }));
  });
  await act(async () => new Promise((resolve) => setTimeout(resolve, 10)));
  assert.equal(requests.length, 1);

  act(() => tree.update(React.createElement(Busca, { termo: 'Ani' })));
  assert.equal(estado.situacao, 'carregando');
  await act(async () => requests[0].resolve({ oponentes: [{ id: 'antigo' }] }));
  assert.equal(estado.situacao, 'carregando');

  act(() => tree.update(React.createElement(Busca, { termo: 'Maria' })));
  await act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  assert.equal(requests.length, 2);
  assert.equal(requests[1].termo, 'Maria');
  await act(async () => requests[1].resolve({ oponentes: [{ id: 'atual' }] }));
  assert.deepEqual(estado, {
    situacao: 'sucesso',
    oponentes: [{ id: 'atual' }],
  });
  act(() => tree.update(React.createElement(Busca, { termo: '@ana' })));
  act(() => tree.update(React.createElement(Busca, { termo: '@' })));
  await act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  assert.equal(requests.length, 3);
  assert.equal(requests[2].termo, '');
  assert.equal(requests[1].signal.aborted, true);
  await act(async () => requests[2].resolve({ oponentes: [] }));
  assert.deepEqual(estado, { situacao: 'sucesso', oponentes: [] });
  act(() => tree.unmount());
});

test('HTTP normaliza arrobas iniciais, espaços e busca vazia', async () => {
  const urls = [];
  const { buscarOponentes } = load('features/desafios/api.ts', {
    '@/api/client': { apiGet: async (url) => urls.push(url) },
  });
  for (const termo of ['@ana', '  @@ana  ', '@', ' @@ ', '', '  ', 'ana@nome'])
    await buscarOponentes(termo);
  assert.deepEqual(urls, [
    '/api/desafios/oponentes?q=ana',
    '/api/desafios/oponentes?q=ana',
    '/api/desafios/oponentes',
    '/api/desafios/oponentes',
    '/api/desafios/oponentes',
    '/api/desafios/oponentes',
    '/api/desafios/oponentes?q=ana%40nome',
  ]);
});
