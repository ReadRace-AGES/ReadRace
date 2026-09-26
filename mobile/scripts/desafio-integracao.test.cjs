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

test('aba navega, recarrega ao foco e ignora respostas antigas', async () => {
  const requests = [];
  const routes = [];
  let focar;
  let desfocar;
  const api = load('features/desafios/api.ts', {
    '@/api/client': {
      apiRequest: (url, options) =>
        new Promise((resolve, reject) =>
          requests.push({ url, options, resolve, reject })
        ),
    },
  });
  const dependencies = {
    'expo-router': {
      useRouter: () => ({ push: (route) => routes.push(route) }),
      useFocusEffect: (callback) => {
        React.useEffect(() => {
          focar = callback;
          desfocar = callback();
          return () => desfocar();
        }, [callback]);
      },
    },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      ScrollView: 'ScrollView',
      Text: 'Text',
      View: 'View',
    },
    '@/api/client': { ApiError: class ApiError extends Error {} },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/Card': { Card: 'Card' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/UsersIcon': { UsersIcon: 'UsersIcon' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    './api': api,
  };
  const { DesafiosScreen } = load(
    'features/desafios/DesafioScreen.tsx',
    dependencies
  );
  let tree;
  const button = (label) =>
    tree.root
      .findAllByType('PrimaryButton')
      .find((node) => node.props.label === label);
  const desafio = (id) => ({
    id,
    status: 'pendente',
    descricao: '150 páginas',
    diasRestantes: 7,
    oponente: { id: 'amigo', username: 'Aninha07', avatarUrl: null },
    progresso: { voce: 0, oponente: 0 },
  });
  const receive = async (index, desafios, nextCursor = null) => {
    await act(async () => requests[index].resolve({ desafios, nextCursor }));
  };

  await act(async () => {
    tree = create(React.createElement(DesafiosScreen));
  });
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/api/desafios');
  assert.deepEqual(Object.keys(requests[0].options).sort(), [
    'method',
    'signal',
  ]);
  assert.equal(requests[0].options.method, 'GET');
  await receive(0, [], 'proxima-pagina');
  assert.equal(tree.root.findAllByType('EmptyState').length, 1);
  assert.equal(requests.length, 1);

  act(() => button('Desafiar Amigo').props.onPress());
  assert.deepEqual(routes, ['/desafiar-amigo']);
  act(() => {
    desfocar();
    desfocar = focar();
  });
  assert.equal(requests.length, 2);
  await receive(1, [desafio('novo')], 'proxima-pagina');
  assert.equal(tree.root.findAllByType('Card').length, 1);
  assert.match(JSON.stringify(tree.toJSON()), /150 páginas/);

  act(() => {
    desfocar();
    desfocar = focar();
  });
  assert.equal(requests.length, 3);
  act(() => {
    desfocar();
    desfocar = focar();
  });
  assert.equal(requests[2].options.signal.aborted, true);
  assert.equal(requests.length, 4);
  await receive(2, [desafio('antigo')]);
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
  await receive(3, [desafio('atual')]);
  assert.equal(tree.root.findAllByType('Card').length, 1);
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 0);

  act(() => {
    desfocar();
    desfocar = focar();
  });
  await act(async () => requests[4].reject(new Error('offline')));
  assert.ok(button('Tentar de novo'));
  act(() => {
    button('Tentar de novo').props.onPress();
  });
  assert.equal(requests.length, 6);
  await receive(5, [desafio('recuperado')]);
  assert.equal(tree.root.findAllByType('Card').length, 1);
  act(() => tree.unmount());
});
