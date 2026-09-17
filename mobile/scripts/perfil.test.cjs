const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
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
const { ApiError } = load('api/client.ts');
const perfil = {
  id: '1',
  nome: 'Daniel',
  titulo: 'Leitor iniciante',
  nivel: 5,
  xpAtual: 2450,
  seguidores: 4,
  seguindo: 2,
  estatisticas: {
    livrosLidos: 5,
    paginasLidas: 6231,
    sequenciaDias: 12,
    conquistas: 1,
  },
  conquistas: [
    {
      id: 'a',
      nome: 'Primeiros Passos',
      descricao: 'Primeira leitura',
      desbloqueada: true,
    },
    { id: 'b', nome: 'Lenda', descricao: 'Meta futura', desbloqueada: false },
  ],
  livrosFavoritos: [
    { id: 'livro', titulo: 'Dune', autor: 'Frank Herbert', capa: null },
  ],
};
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (v) => v },
};
const dependencies = {
  'react-native': native,
  'expo-image': { Image: 'Image' },
  'expo-router': {},
  '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
  '@/components/avatar': { Avatar: 'Avatar' },
  '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
  '@/components/toast-provider': {},
  './usePerfil': {},
};
for (const name of [
  'Card',
  'AppHeader',
  'BookCover',
  'EmptyState',
  'PrimaryButton',
])
  dependencies[`@/components/${name}`] = { [name]: name };

test('perfil exibe dados persistidos, conquistas bloqueadas e quatro ações de Toast', () => {
  const { PerfilConteudo } = load(
    'features/perfil/PerfilScreen.tsx',
    dependencies
  );
  let tree,
    calls = 0;
  act(() => {
    tree = create(
      React.createElement(PerfilConteudo, {
        perfil,
        onPlaceholder: () => calls++,
      })
    );
  });
  const text = JSON.stringify(tree.toJSON());
  assert.match(text, /2.450/);
  assert.match(text, /6.231/);
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some(
        (node) =>
          React.Children.toArray(node.props.children).join('') ===
          '“Leitor iniciante”'
      )
  );
  assert.match(text, /Bloqueada/);
  assert.doesNotMatch(text, /Mascotes|Configurações|adicionar favorito/);
  act(() => {
    tree.root
      .findAllByType('Pressable')
      .forEach((button) => button.props.onPress());
    tree.root.findByType('BookCover').props.onPress();
  });
  assert.equal(calls, 4);
  act(() =>
    tree.update(
      React.createElement(PerfilConteudo, {
        perfil: { ...perfil, livrosFavoritos: [] },
        onPlaceholder: () => {},
      })
    )
  );
  assert.doesNotMatch(JSON.stringify(tree.toJSON()), /Livros favoritos/);
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  act(() => tree.unmount());
});

test('carregamento e erro mantêm voltar e permitem nova tentativa', () => {
  let estado = { situacao: 'carregando' };
  let retries = 0,
    backs = 0,
    tree;
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': {
      useRouter: () => ({ canGoBack: () => true, back: () => backs++ }),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => {} }),
    },
    './usePerfil': {
      usePerfil: () => ({ estado, recarregar: () => retries++ }),
    },
  });
  act(() => {
    tree = create(React.createElement(PerfilScreen, { usuarioId: '1' }));
  });
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.equal(backs, 1);
  estado = { situacao: 'erro', mensagem: 'Usuário não encontrado.' };
  act(() => tree.update(React.createElement(PerfilScreen, { usuarioId: '1' })));
  const empty = tree.root.findByType('EmptyState');
  assert.equal(empty.props.message, estado.mensagem);
  act(() => empty.props.action.props.onPress());
  assert.equal(retries, 1);
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.equal(backs, 2);
  act(() => tree.unmount());
});

test('API usa somente o usuário exibido e encaminha cancelamento', async () => {
  let request;
  const { buscarPerfil } = load('features/perfil/api.ts', {
    '@/api/client': {
      apiGet: (...args) => {
        request = args;
        return Promise.resolve(perfil);
      },
    },
  });
  const controller = new AbortController();
  await buscarPerfil('outro/id', controller.signal);
  assert.equal(request[0], '/api/usuarios/outro%2Fid/perfil');
  assert.deepEqual(request[1], { signal: controller.signal });
});

test('hook cancela perfil antigo, ignora resposta atrasada e permite tentar novamente', async () => {
  const pending = [];
  const { usePerfil } = load('features/perfil/usePerfil.ts', {
    '@/api/client': { ApiError },
    './api': {
      buscarPerfil: (id, signal) =>
        new Promise((resolve, reject) =>
          pending.push({ id, signal, resolve, reject })
        ),
    },
  });
  let current, tree;
  function Probe({ id }) {
    current = usePerfil(id);
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe, { id: '1' }));
  });
  assert.equal(current.estado.situacao, 'carregando');
  await act(async () => tree.update(React.createElement(Probe, { id: '2' })));
  assert.equal(pending[0].signal.aborted, true);
  await act(async () => pending[1].resolve({ ...perfil, id: '2' }));
  await act(async () => pending[0].resolve(perfil));
  assert.equal(current.estado.dados.id, '2');
  await act(async () => current.recarregar());
  await act(async () => pending[2].reject(new Error('offline')));
  assert.equal(current.estado.situacao, 'erro');
  await act(async () => current.recarregar());
  await act(async () => pending[3].resolve({ ...perfil, id: '2' }));
  assert.equal(current.estado.situacao, 'sucesso');
  act(() => tree.unmount());
  assert.equal(pending[3].signal.aborted, true);
});
