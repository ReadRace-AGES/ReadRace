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
const model = load('features/forum/model.ts');
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
native.StyleSheet = { create: (v) => v };
const theme = {
  colors: {},
  radius: {},
  sizes: {},
  spacing: {},
  textStyles: {},
  typography: { fontFamily: {} },
};
const post = (id, totalCurtidas = 0, curtidoPorMim = false) => ({
  id,
  autor: { id: 'u1', nome: 'Ana', avatarUrl: null, sequenciaDias: 3 },
  publicadoEm: '2026-10-15T12:00:00Z',
  texto: id,
  totalCurtidas,
  curtidoPorMim,
});
const forum = (
  podeEscrever = true,
  posts = [post('antigo', 5), post('curtido', 8, true)],
  id = 'c1'
) => ({
  clube: {
    id,
    nome: id,
    livroAtual: { titulo: 'Livro', autor: 'Autor', capaUrl: null },
  },
  posts,
  podeEscrever,
});
const deferred = () => {
  let resolve, reject;
  const promise = new Promise((ok, fail) => {
    resolve = ok;
    reject = fail;
  });
  return { promise, resolve, reject };
};

async function mount(dados = forum()) {
  const h = { gets: [], posts: [], likes: [], erros: [] };
  const request = (list, args) => {
    const d = deferred();
    list.push({ ...args, ...d });
    return d.promise;
  };
  const api = {
    buscarForumDoClube: (clubeId, signal) =>
      request(h.gets, { clubeId, signal }),
    publicarPostDoClube: (clubeId, texto, signal) =>
      request(h.posts, { clubeId, texto, signal }),
  };
  const { useForumClube } = load('features/forum/useForumClube.ts', {
    '@/api/client': client,
    './api': api,
    '@/features/posts/api': {
      curtir: (id, signal) => request(h.likes, { id, signal, curtir: true }),
      descurtir: (id, signal) =>
        request(h.likes, { id, signal, curtir: false }),
    },
  });
  const components = Object.fromEntries(
    ['BottomSheet', 'PrimaryButton', 'AppHeader', 'EmptyState', 'PostCard'].map(
      (name) => [`@/components/${name}`, { [name]: name }]
    )
  );
  const { NovoPostSheet } = load('features/forum/NovoPostSheet.tsx', {
    'react-native': native,
    '@/theme': theme,
    '@/api/client': client,
    ...components,
    './api': api,
    './model': model,
  });
  const { ForumClubeScreen } = load('features/forum/ForumClubeScreen.tsx', {
    'react-native': native,
    '@/theme': theme,
    ...components,
    'expo-router': { useRouter: () => ({}) },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showErrorToast: (error) => h.erros.push(error),
      }),
    },
    '@/features/livro/model': { tempoRelativo: () => '' },
    './NovoPostSheet': { NovoPostSheet },
    './useForumClube': {
      useForumClube: (id) => {
        h.state = useForumClube(id);
        return h.state;
      },
    },
  });
  h.render = (clubeId) => React.createElement(ForumClubeScreen, { clubeId });
  await act(async () => {
    h.tree = create(h.render(dados.clube.id));
  });
  await act(async () => h.gets[0].resolve(dados));
  h.all = (type) => h.tree.root.findAllByType(type);
  h.button = (label) =>
    h.all('PrimaryButton').find((item) => item.props.label === label);
  h.input = () => h.tree.root.findByType('TextInput');
  h.open = () => act(async () => h.button('Escrever post').props.onPress());
  h.type = (texto) => act(async () => h.input().props.onChangeText(texto));
  h.send = () =>
    act(async () => {
      h.button('Publicar').props.onPress();
    });
  h.dispose = () => act(async () => h.tree.unmount());
  return h;
}

test('texto: limites UTF-16, emoji e conjunto exato de branco do backend', () => {
  for (const texto of [
    '',
    ' ',
    '\t\r\n',
    '\u00a0',
    'a'.repeat(2001),
    'a'.repeat(1999) + '😀',
  ])
    assert.equal(model.textoPostValido(texto), false, JSON.stringify(texto));
  for (const texto of [
    'a',
    'a'.repeat(2000),
    '😀',
    '😀'.repeat(1000),
    'a'.repeat(1998) + '😀',
    ' \ntexto\t ',
    '\u0085',
    '\u200b',
    '\ufeff',
  ])
    assert.equal(model.textoPostValido(texto), true, JSON.stringify(texto));
  const brancos = [
    9,
    10,
    11,
    12,
    13,
    28,
    29,
    30,
    31,
    32,
    160,
    5760,
    ...Array.from({ length: 11 }, (_, i) => 8192 + i),
    8232,
    8233,
    8239,
    8287,
    12288,
  ];
  for (const codigo of brancos)
    assert.equal(
      model.textoPostValido(String.fromCodePoint(codigo)),
      false,
      `U+${codigo.toString(16)}`
    );
});

test('API publica somente texto original com autenticação, signal e erro do envelope', async () => {
  const original = global.fetch;
  const calls = [];
  client.configurarAutenticacao({
    obterToken: async () => 'token',
    renovarToken: async () => null,
    aoPerderSessao: async () => {},
  });
  const api = load('features/forum/api.ts', { '@/api/client': client });
  const signal = new AbortController().signal;
  const texto = ' \tMinha leitura\r\n\u00a0 ';
  global.fetch = async (url, init) => {
    calls.push({ url, ...init });
    return Response.json(post('novo'), { status: 201 });
  };
  try {
    assert.deepEqual(
      await api.publicarPostDoClube('clube/1', texto, signal),
      post('novo')
    );
    assert.ok(calls[0].url.endsWith('/api/clubes/clube%2F1/posts'));
    assert.equal(calls[0].method, 'POST');
    assert.equal(calls[0].signal, signal);
    assert.equal(calls[0].headers.get('Authorization'), 'Bearer token');
    assert.equal(calls[0].headers.get('Content-Type'), 'application/json');
    assert.deepEqual(JSON.parse(calls[0].body), { texto });
    global.fetch = async () =>
      Response.json(
        { code: 'SO_MEMBRO_POSTA', message: 'Somente membros.' },
        { status: 403 }
      );
    await assert.rejects(
      api.publicarPostDoClube('c1', texto),
      (err) =>
        err instanceof client.ApiError &&
        err.message === 'Somente membros.' &&
        err.code === 'SO_MEMBRO_POSTA'
    );
  } finally {
    global.fetch = original;
    client.configurarAutenticacao(null);
  }
});

test('entrada outline aparece exclusivamente com permissão, em fórum cheio ou vazio', async () => {
  for (const permitido of [true, false])
    for (const posts of [[], [post('antigo')]]) {
      const h = await mount(forum(permitido, posts));
      try {
        assert.equal(!!h.button('Escrever post'), permitido);
        if (permitido)
          assert.equal(h.button('Escrever post').props.variant, 'outline');
        assert.equal(h.all('EmptyState').length, posts.length === 0 ? 1 : 0);
      } finally {
        await h.dispose();
      }
    }
});

test('painel não trunca texto, desabilita inválidos e descarta ao fechar', async () => {
  const h = await mount();
  try {
    await h.open();
    assert.equal(h.input().props.multiline, true);
    assert.equal(h.input().props.maxLength, undefined);
    for (const texto of [
      '',
      ' \t\r\n\u00a0',
      'a'.repeat(2001),
      '😀'.repeat(1000) + 'a',
    ]) {
      await h.type(texto);
      assert.equal(h.input().props.value, texto);
      assert.equal(h.button('Publicar').props.disabled, true);
      await h.send();
    }
    assert.equal(h.posts.length, 0);
    for (const texto of ['a', '😀'.repeat(1000)]) {
      await h.type(texto);
      assert.equal(h.button('Publicar').props.disabled, false);
    }
    await act(async () =>
      h.tree.root.findByType('BottomSheet').props.onClose()
    );
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, false);
    assert.equal(h.input().props.value, '');
    await h.open();
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, true);
    assert.equal(h.input().props.value, '');
  } finally {
    await h.dispose();
  }
});

test('envio tem trava síncrona; falha exibe envelope sem descartar texto ou painel', async () => {
  const h = await mount();
  try {
    await h.open();
    const texto = ' \tLeitura\r\n\u00a0 ';
    await h.type(texto);
    await act(async () => {
      const submit = h.button('Publicar').props.onPress;
      submit();
      submit();
      h.tree.root.findByType('BottomSheet').props.onClose();
      h.input().props.onChangeText('alterado');
    });
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].texto, texto);
    assert.equal(h.button('Publicar').props.loading, true);
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, true);
    assert.equal(h.input().props.editable, false);
    assert.equal(
      h.tree.root.findByType('BottomSheet').props.dismissible,
      false
    );
    assert.equal(h.tree.root.findByType('Pressable').props.disabled, true);
    await act(async () =>
      h.posts[0].reject(
        new client.ApiError(400, {
          code: 'POST_INVALIDO',
          message: 'Mensagem do servidor.',
        })
      )
    );
    assert.equal(h.input().props.value, texto);
    assert.equal(h.button('Publicar').props.loading, false);
    assert.equal(
      h.tree.root.findByProps({ accessibilityRole: 'alert' }).props.children,
      'Mensagem do servidor.'
    );
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, true);
    assert.equal(h.gets.length, 1);
    await act(async () =>
      h.tree.root.findByType('BottomSheet').props.onClose()
    );
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, false);
    assert.equal(h.input().props.value, '');
    assert.equal(
      h.all('Text').some((item) => item.props.accessibilityRole === 'alert'),
      false
    );
    await h.open();
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, true);
    assert.equal(h.input().props.value, '');
    assert.equal(
      h.all('Text').some((item) => item.props.accessibilityRole === 'alert'),
      false
    );
  } finally {
    await h.dispose();
  }
});

test('sucesso insere no topo sem GET, preserva curtidas pendentes e permite curtir o novo post', async () => {
  const h = await mount();
  try {
    let like;
    await act(async () => {
      like = h.state.alternarCurtida('antigo');
    });
    await h.open();
    await h.type(' Novo post\n ');
    await h.send();
    const novo = { ...post('novo'), texto: h.posts[0].texto };
    await act(async () => h.posts[0].resolve(novo));
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, false);
    assert.equal(h.input().props.value, '');
    assert.equal(h.gets.length, 1);
    assert.deepEqual(h.state.forum.dados.posts, [
      novo,
      post('antigo', 6, true),
      post('curtido', 8, true),
    ]);
    await act(async () => {
      h.likes[0].reject(
        new client.ApiError(500, { message: 'Falha na curtida' })
      );
      await assert.rejects(like, /Falha na curtida/);
    });
    assert.deepEqual(h.state.forum.dados.posts, [
      novo,
      post('antigo', 5),
      post('curtido', 8, true),
    ]);
    await act(async () => {
      h.all('PostCard')[0].props.onLikePress();
    });
    assert.equal(h.likes[1].id, 'novo');
    await act(async () =>
      h.likes[1].resolve({ totalCurtidas: 1, curtidoPorMim: true })
    );
    assert.equal(h.all('PostCard')[0].props.likes, 1);
    await h.open();
    assert.equal(h.input().props.value, '');
  } finally {
    await h.dispose();
  }
});

for (const falha of [false, true])
  test(`GET pendente (${falha ? 'falha' : 'sucesso'}) não remove publicação ou curtidas locais`, async () => {
    const h = await mount();
    try {
      await act(async () => h.state.recarregar());
      assert.equal(h.gets.length, 2);
      await h.open();
      await h.type('Novo');
      await h.send();
      await act(async () => {
        h.posts[0].resolve(post('novo'));
        h.state.alternarCurtida('antigo');
      });
      await act(async () =>
        falha
          ? h.gets[1].reject(new Error('GET falhou'))
          : h.gets[1].resolve(forum())
      );
      assert.deepEqual(
        h.state.forum.dados.posts.map((p) => p.id),
        ['novo', 'antigo', 'curtido']
      );
      assert.equal(h.state.forum.dados.posts[1].totalCurtidas, 6);
      await act(async () =>
        h.likes[0].resolve({ totalCurtidas: 6, curtidoPorMim: true })
      );
    } finally {
      await h.dispose();
    }
  });

test('GET iniciado durante curtida pendente não reverte a curtida', async () => {
  const h = await mount();
  try {
    await act(async () => {
      h.state.alternarCurtida('antigo');
    });
    await act(async () => h.state.recarregar());
    await act(async () =>
      h.likes[0].resolve({ totalCurtidas: 6, curtidoPorMim: true })
    );
    await act(async () => h.gets[1].resolve(forum()));
    assert.equal(h.state.forum.dados.posts[0].totalCurtidas, 6);
  } finally {
    await h.dispose();
  }
});

test('troca de clube e desmontagem cancelam requisições e ignoram respostas antigas', async () => {
  const h = await mount();
  try {
    await h.open();
    await h.type('Post antigo');
    await h.send();
    await act(async () => {
      h.state.alternarCurtida('antigo');
    });
    await act(async () => h.state.recarregar());
    await act(async () => h.tree.update(h.render('c2')));
    for (const req of [h.gets[1], h.posts[0], h.likes[0]])
      assert.equal(req.signal.aborted, true);
    await act(async () => {
      h.gets[1].resolve(forum());
      h.posts[0].resolve(post('tardio'));
      h.likes[0].reject(new Error('Falha antiga'));
      h.gets[2].resolve(forum(true, [], 'c2'));
    });
    assert.equal(h.state.forum.dados.clube.id, 'c2');
    assert.deepEqual(h.state.forum.dados.posts, []);
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, false);
    assert.equal(h.input().props.value, '');
    assert.deepEqual(h.erros, []);
    await h.open();
    assert.equal(h.tree.root.findByType('BottomSheet').props.visible, true);
    assert.equal(h.input().props.value, '');
    await h.type('Post novo');
    await h.send();
    await h.dispose();
    assert.equal(h.posts[1].signal.aborted, true);
    await act(async () => h.posts[1].resolve(post('apos-desmontagem')));
  } finally {
    if (h.tree.toJSON()) await h.dispose();
  }
});
