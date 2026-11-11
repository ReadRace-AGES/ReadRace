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

const client = load('api/client.ts');
const native = Object.fromEntries(
  [
    'View',
    'Text',
    'TextInput',
    'Pressable',
    'ScrollView',
    'ActivityIndicator',
  ].map((name) => [name, name])
);
native.StyleSheet = { create: (styles) => styles };
const theme = {
  colors: { danger: 'danger' },
  radius: {},
  sizes: {},
  spacing: {},
  textStyles: {},
};
const { NovoPostSheet } = load('features/forum/NovoPostSheet.tsx', {
  'react-native': native,
  '@/api/client': client,
  '@/components/BottomSheet': { BottomSheet: 'BottomSheet' },
  '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '@/theme': theme,
  './model': load('features/forum/model.ts'),
});
function deferred() {
  let resolve, reject;
  const promise = new Promise((ok, fail) => {
    resolve = ok;
    reject = fail;
  });
  return { promise, resolve, reject };
}
const post = (id) => ({
  id,
  autor: { id: 'u1', nome: 'Leitor', avatarUrl: null },
  publicadoEm: '2026-11-08T12:00:00Z',
  texto: id,
  totalCurtidas: 0,
  curtidoPorMim: false,
  totalComentarios: 0,
});
const dados = (id = 'c1', sigo = true, posts = [post('antigo')]) => ({
  id,
  nome: id,
  imagemUrl: null,
  seguidores: 10,
  sigo,
  posts,
});

async function montar({
  sigo = true,
  origem = '/feed',
  posts = [post('antigo')],
} = {}) {
  const h = { gets: [], posts: [], likes: [], joins: [], errors: [] };
  const request = (list, args) => {
    const d = deferred();
    list.push({ ...args, ...d });
    return d.promise;
  };
  const api = {
    buscarComunidade: (id, signal) => request(h.gets, { id, signal }),
    publicarPostDaComunidade: (id, texto, signal) =>
      request(h.posts, { id, texto, signal }),
    seguirComunidade: (id) => request(h.joins, { id }),
    deixarDeSeguirComunidade: (id) => request(h.joins, { id }),
  };
  const { useComunidade } = load('features/comunidade/useComunidade.ts', {
    '@/api/client': client,
    './api': api,
    '@/features/posts/api': {
      curtir: (id) => request(h.likes, { id }),
      descurtir: (id) => request(h.likes, { id }),
    },
  });
  const { ComunidadeScreen } = load(
    'features/comunidade/ComunidadeScreen.tsx',
    {
      'react-native': native,
      'expo-image': { Image: 'Image' },
      'react-native-safe-area-context': {
        useSafeAreaInsets: () => ({ top: 0 }),
      },
      'react-native-svg': { __esModule: true, default: 'Svg', Path: 'Path' },
      '@/api/client': client,
      '@/components/EmptyState': { EmptyState: 'EmptyState' },
      '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
      '@/components/PostCard': { PostCard: 'PostCard' },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
      '@/components/toast-provider': {
        useToastContext: () => ({
          showToast: () => {},
          showErrorToast: (error) => h.errors.push(error),
        }),
      },
      '@/components/useVoltar': { useVoltar: () => () => {} },
      '@/features/comentarios/ComentariosSheet': {
        ComentariosSheet: 'ComentariosSheet',
      },
      '@/features/forum/NovoPostSheet': { NovoPostSheet },
      '@/features/livro/model': { tempoRelativo: () => '' },
      '@/theme': theme,
      './ComunidadeScreen.styles': { styles: {} },
      './api': api,
      './useComunidade': {
        useComunidade: (id) => {
          h.state = useComunidade(id);
          return h.state;
        },
      },
    }
  );
  let rotaId = 'c1';
  const { default: Route } = load(
    `app/(tabs)/(${origem === '/feed' ? 'feed' : 'busca'})/comunidade/[comunidadeId].tsx`,
    {
      'expo-router': { useLocalSearchParams: () => ({ comunidadeId: rotaId }) },
      '@/features/comunidade/ComunidadeScreen': { ComunidadeScreen },
    }
  );
  h.render = (id) => {
    rotaId = id;
    return React.createElement(Route);
  };
  await act(async () => {
    h.tree = create(h.render('c1'));
  });
  await act(async () => h.gets[0].resolve(dados('c1', sigo, posts)));
  h.all = (type) => h.tree.root.findAllByType(type);
  h.button = (label) =>
    h.all('PrimaryButton').find((node) => node.props.label === label);
  h.input = () => h.tree.root.findByType('TextInput');
  h.sheet = () => h.tree.root.findByType('BottomSheet');
  h.open = () => act(async () => h.button('Escrever post').props.onPress());
  h.type = (text) => act(async () => h.input().props.onChangeText(text));
  h.send = () =>
    act(async () => {
      h.button('Publicar').props.onPress();
    });
  h.dispose = () => act(async () => h.tree.unmount());
  return h;
}

test('API da comunidade envia conteudo original com token, destino codificado e signal', async () => {
  const original = global.fetch;
  const calls = [];
  client.configurarAutenticacao({
    obterToken: async () => 'token',
    renovarToken: async () => null,
    aoPerderSessao: async () => {},
  });
  const api = load('features/comunidade/api.ts', { '@/api/client': client });
  const signal = new AbortController().signal;
  const conteudo = ' \tMinha leitura\r\n😀 ';
  global.fetch = async (url, init) => {
    calls.push({ url, ...init });
    return Response.json(post('novo'), { status: 201 });
  };
  try {
    assert.deepEqual(
      await api.publicarPostDaComunidade('comunidade/1', conteudo, signal),
      post('novo')
    );
    assert.ok(calls[0].url.endsWith('/api/comunidades/comunidade%2F1/posts'));
    assert.equal(calls[0].method, 'POST');
    assert.equal(calls[0].signal, signal);
    assert.equal(calls[0].headers.get('Authorization'), 'Bearer token');
    assert.deepEqual(JSON.parse(calls[0].body), { conteudo });
    global.fetch = async () =>
      Response.json(
        { code: 'SO_MEMBRO_POSTA', message: 'Somente participantes.' },
        { status: 403 }
      );
    await assert.rejects(
      api.publicarPostDaComunidade('c1', conteudo),
      (err) => err instanceof client.ApiError && err.code === 'SO_MEMBRO_POSTA'
    );
  } finally {
    global.fetch = original;
    client.configurarAutenticacao(null);
  }
});

for (const origem of ['/feed', '/buscar'])
  for (const sigo of [true, false])
    test(`${origem}: Escrever post aparece somente para participante, inclusive sem posts (${sigo})`, async () => {
      const h = await montar({ origem, sigo, posts: [] });
      try {
        assert.equal(!!h.button('Escrever post'), sigo);
        assert.equal(h.sheet().props.visible, false);
        if (sigo) {
          await h.open();
          assert.equal(h.sheet().props.visible, true);
        }
      } finally {
        await h.dispose();
      }
    });

test('comunidade: loading, trava síncrona e erro 403 preservam painel, texto e colors.danger', async () => {
  const h = await montar();
  try {
    await h.open();
    const texto = ' Leitura\n😀 ';
    await h.type(texto);
    await act(async () => {
      const submit = h.button('Publicar').props.onPress;
      submit();
      submit();
      h.sheet().props.onClose();
      h.input().props.onChangeText('não deve mudar');
    });
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].id, 'c1');
    assert.equal(h.posts[0].texto, texto);
    assert.equal(h.button('Publicar').props.loading, true);
    assert.equal(h.sheet().props.dismissible, false);
    assert.equal(h.input().props.editable, false);
    await act(async () =>
      h.posts[0].reject(
        new client.ApiError(403, {
          code: 'SO_MEMBRO_POSTA',
          message: 'Somente participantes.',
        })
      )
    );
    assert.equal(h.sheet().props.visible, true);
    assert.equal(h.input().props.value, texto);
    assert.equal(h.button('Publicar').props.loading, false);
    const alerta = h.tree.root.findByProps({ accessibilityRole: 'alert' });
    assert.equal(alerta.props.children, 'Somente participantes.');
    assert.equal(alerta.props.style.color, theme.colors.danger);
    await act(async () => {
      h.state.alternarSeguimento();
    });
    assert.equal(h.button('Escrever post'), undefined);
    await act(async () => h.joins[0].resolve({ sigo: false, seguidores: 9 }));
    assert.equal(h.sheet().props.visible, true);
    assert.equal(h.input().props.value, texto);
    await act(async () => h.sheet().props.onClose());
    assert.equal(h.input().props.value, '');
  } finally {
    await h.dispose();
  }
});

test('sucesso insere no topo sem GET e preserva curtidas, comentários e participação concorrentes', async () => {
  const h = await montar();
  try {
    await h.open();
    await h.type('Novo');
    await h.send();
    let curtida, participacao;
    await act(async () => {
      curtida = h.state.alternarCurtida('antigo');
      h.state.somarComentario('antigo');
      participacao = h.state.alternarSeguimento();
    });
    await act(async () => {
      h.joins[0].resolve({ sigo: false, seguidores: 9 });
      await participacao;
    });
    await act(async () => h.posts[0].resolve(post('novo')));
    assert.equal(h.sheet().props.visible, false);
    assert.equal(h.input().props.value, '');
    assert.equal(h.gets.length, 1);
    assert.deepEqual(
      h.state.comunidade.dados.posts.map((p) => p.id),
      ['novo', 'antigo']
    );
    assert.equal(h.state.comunidade.dados.posts[1].totalCurtidas, 1);
    assert.equal(h.state.comunidade.dados.posts[1].totalComentarios, 1);
    await act(async () => {
      h.likes[0].resolve({ totalCurtidas: 7, curtidoPorMim: true });
      await curtida;
    });
    await act(async () => h.state.adicionarPost(post('novo')));
    assert.equal(h.state.comunidade.dados.posts.length, 2);
    assert.equal(h.state.comunidade.dados.posts[1].totalCurtidas, 7);
    assert.equal(h.state.comunidade.dados.posts[1].totalComentarios, 1);
    assert.equal(h.state.comunidade.dados.seguidores, 9);
    assert.equal(h.state.comunidade.dados.sigo, false);
    assert.equal(h.button('Escrever post'), undefined);
  } finally {
    await h.dispose();
  }
});

test('troca de comunidade cancela envio e ignora resposta antiga; desmontagem também cancela', async () => {
  const h = await montar();
  try {
    await h.open();
    await h.type('Rascunho c1');
    await h.send();
    await act(async () => h.tree.update(h.render('c2')));
    assert.equal(h.posts[0].signal.aborted, true);
    await act(async () => {
      h.posts[0].resolve(post('antigo tardio'));
      h.gets[1].resolve(dados('c2'));
    });
    assert.equal(h.sheet().props.visible, false);
    assert.equal(h.input().props.value, '');
    assert.deepEqual(
      h.state.comunidade.dados.posts.map((p) => p.id),
      ['antigo']
    );
    await h.open();
    await h.type('Rascunho c2');
    await h.send();
    await h.dispose();
    assert.equal(h.posts[1].signal.aborted, true);
    await act(async () => h.posts[1].resolve(post('desmontado')));
  } finally {
    if (h.tree.toJSON()) await h.dispose();
  }
});

test('painel compartilhado isola clube e comunidade com mesmo ID e ignora envio de abertura anterior', async () => {
  const calls = [],
    sucessos = [];
  let destinoId = 'clube:igual',
    visible = true,
    fechamentos = 0;
  const render = () =>
    React.createElement(NovoPostSheet, {
      destinoId,
      visible,
      publicar: (texto, signal) => {
        const d = deferred();
        calls.push({ ...d, texto, signal });
        return d.promise;
      },
      onSuccess: (p) => sucessos.push(p),
      onClose: () => fechamentos++,
    });
  let tree;
  await act(async () => {
    tree = create(render());
  });
  const input = () => tree.root.findByType('TextInput');
  const send = () => tree.root.findByType('PrimaryButton').props.onPress();
  try {
    await act(async () => input().props.onChangeText('Clube'));
    await act(async () => {
      send();
    });
    destinoId = 'comunidade:igual';
    await act(async () => tree.update(render()));
    assert.equal(calls[0].signal.aborted, true);
    assert.equal(input().props.value, '');
    await act(async () => input().props.onChangeText('Comunidade'));
    await act(async () => {
      send();
    });
    visible = false;
    await act(async () => tree.update(render()));
    assert.equal(calls[1].signal.aborted, true);
    visible = true;
    await act(async () => tree.update(render()));
    await act(async () => input().props.onChangeText('Novo rascunho'));
    await act(async () => {
      calls[0].resolve(post('clube'));
      calls[1].reject(new Error('falha antiga'));
    });
    assert.deepEqual(sucessos, []);
    assert.equal(fechamentos, 0);
    assert.equal(input().props.value, 'Novo rascunho');
    assert.equal(tree.root.findByType('PrimaryButton').props.loading, false);
    assert.equal(
      tree.root.findAllByProps({ accessibilityRole: 'alert' }).length,
      0
    );
  } finally {
    await act(async () => tree.unmount());
  }
});
