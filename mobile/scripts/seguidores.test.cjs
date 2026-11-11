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
const eu = { id: 'eu', nome: 'Eu', nomeUsuario: 'eu', avatarUrl: null };
const outro = {
  id: 'outro',
  nome: 'Outro',
  nomeUsuario: 'outro',
  avatarUrl: null,
};

test('API usa as rotas existentes de usuários e encaminha cancelamento', async () => {
  const chamadas = [];
  const { buscarLista } = load('features/seguidores/api.ts', {
    '@/api/client': {
      apiGet: async (...args) => {
        chamadas.push(args);
        return { usuarios: [] };
      },
    },
  });
  const controller = new AbortController();
  for (const tipo of ['seguidores', 'seguindo'])
    await buscarLista('id/1', tipo, controller.signal);
  assert.deepEqual(
    chamadas.map((c) => c[0]),
    ['/api/usuarios/id%2F1/seguidores', '/api/usuarios/id%2F1/seguindo']
  );
  assert.equal(chamadas[0][1].signal, controller.signal);
});

for (const origem of ['perfil', 'busca']) {
  test(`lista na aba ${origem}: estados, cabeçalho, navegação e recarga no retorno`, async () => {
    let carga = { situacao: 'carregando' },
      foco,
      recargas = 0;
    const rotas = [],
      params = { usuarioId: 'outro', tipo: 'seguidores' };
    const { ListaSeguidoresScreen } = load(
      'features/seguidores/ListaSeguidoresScreen.tsx',
      {
        'react-native': {
          View: 'View',
          ActivityIndicator: 'ActivityIndicator',
          FlatList: 'FlatList',
        },
        'expo-router': {
          useLocalSearchParams: () => params,
          useFocusEffect: (cb) => {
            foco = cb;
          },
          useRouter: () => ({
            push: (rota) => rotas.push(rota),
            navigate: (rota) => rotas.push(rota),
            canGoBack: () => true,
            back: () => {},
          }),
        },
        '@/components/AppHeader': { AppHeader: 'AppHeader' },
        '@/components/EmptyState': { EmptyState: 'EmptyState' },
        '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
        '@/components/LinhaUsuario': { LinhaUsuario: 'LinhaUsuario' },
        '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
        '@/theme': { colors: {}, spacing: {} },
        './useListaSeguidores': {
          useListaSeguidores: () => ({ carga, recarregar: () => recargas++ }),
        },
      }
    );
    let tree;
    const atualizar = () =>
      act(async () =>
        tree.update(React.createElement(ListaSeguidoresScreen, { origem }))
      );
    await act(async () => {
      tree = create(React.createElement(ListaSeguidoresScreen, { origem }));
    });
    try {
      assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
      await act(async () => foco());
      assert.equal(recargas, 0);
      carga = { situacao: 'erro', mensagem: 'Erro de rede' };
      await atualizar();
      const erro = tree.root.findByType('EmptyState');
      assert.equal(erro.props.message, 'Erro de rede');
      erro.props.action.props.onPress();
      assert.equal(recargas, 1);
      carga = {
        situacao: 'sucesso',
        nome: 'Outro',
        eu: 'eu',
        usuarios: [outro, eu],
      };
      await atualizar();
      assert.equal(tree.root.findByType('AppHeader').props.subtitle, 'Outro');
      const lista = tree.root.findByType('FlatList');
      assert.equal(
        lista.props.ListEmptyComponent.props.message,
        'Ninguém segue este perfil ainda.'
      );
      lista.props.renderItem({ item: outro }).props.onPress();
      lista.props.renderItem({ item: eu }).props.onPress();
      assert.deepEqual(rotas, [
        origem === 'perfil'
          ? '/(tabs)/(perfil)/perfil-usuario/outro'
          : '/(tabs)/(busca)/usuario/outro',
        '/(tabs)/(perfil)/perfil',
      ]);
      params.tipo = 'seguindo';
      carga = { ...carga, usuarios: [] };
      await atualizar();
      assert.equal(tree.root.findByType('AppHeader').props.title, 'Seguindo');
      assert.equal(
        tree.root.findByType('FlatList').props.ListEmptyComponent.props.message,
        'Este perfil ainda não segue ninguém.'
      );
      await act(async () => foco());
      assert.equal(recargas, 2);
    } finally {
      await act(async () => tree.unmount());
    }
  });
}

test('hook cancela carga antiga, trata erro e permite tentar novamente', async () => {
  const chamadas = [];
  const { useListaSeguidores } = load(
    'features/seguidores/useListaSeguidores.ts',
    {
      '@/api/client': load('api/client.ts'),
      '@/features/perfil/api': {
        buscarPerfil: async (id) => ({ nome: id }),
        buscarUsuarioAtual: async () => ({ id: 'eu' }),
      },
      './api': {
        buscarLista: (id, tipo, signal) =>
          new Promise((resolve, reject) =>
            chamadas.push({ id, tipo, signal, resolve, reject })
          ),
      },
    }
  );
  let atual;
  function Probe({ id }) {
    atual = useListaSeguidores(id, 'seguindo');
    return null;
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Probe, { id: 'primeiro' }));
  });
  try {
    await act(async () =>
      tree.update(React.createElement(Probe, { id: 'segundo' }))
    );
    assert.equal(chamadas[0].signal.aborted, true);
    await act(async () => chamadas[0].resolve({ usuarios: [outro] }));
    assert.equal(atual.carga.situacao, 'carregando');
    await act(async () => chamadas[1].reject(new Error('Falha')));
    assert.equal(atual.carga.situacao, 'erro');
    await act(async () => atual.recarregar());
    assert.equal(atual.carga.situacao, 'carregando');
    await act(async () => chamadas[2].resolve({ usuarios: [eu] }));
    assert.deepEqual(atual.carga, {
      situacao: 'sucesso',
      nome: 'segundo',
      eu: 'eu',
      usuarios: [eu],
    });
  } finally {
    await act(async () => tree.unmount());
  }
  assert.equal(chamadas[2].signal.aborted, true);
});

test('linha compartilhada exibe avatar, nome e username, sem botão de seguir', async () => {
  const { LinhaUsuario } = load('components/LinhaUsuario.tsx', {
    'react-native': { Pressable: 'Pressable', Text: 'Text', View: 'View' },
    './avatar': { Avatar: 'Avatar' },
    '@/theme': { sizes: { avatarLarge: 50 } },
  });
  let tree,
    toques = 0;
  await act(async () => {
    tree = create(
      React.createElement(LinhaUsuario, { ...outro, onPress: () => toques++ })
    );
  });
  try {
    assert.equal(tree.root.findByType('Avatar').props.name, 'Outro');
    assert.equal(tree.root.findAllByType('Pressable').length, 1);
    assert.match(JSON.stringify(tree.toJSON()), /@/);
    tree.root.findByType('Pressable').props.onPress();
    assert.equal(toques, 1);
  } finally {
    await act(async () => tree.unmount());
  }
});
