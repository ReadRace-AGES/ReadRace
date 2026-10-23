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
  ['View', 'Text', 'TextInput', 'Pressable', 'ActivityIndicator'].map((n) => [
    n,
    n,
  ])
);
native.StyleSheet = { create: (v) => v };
native.useWindowDimensions = () => ({ width: 400, height: 800 });
const theme = {
  colors: {},
  radius: {},
  sizes: { inputHeight: 40, iconSmall: 16 },
  spacing: {},
  textStyles: {},
};
const deferred = () => {
  let resolve, reject;
  const promise = new Promise((ok, fail) => {
    resolve = ok;
    reject = fail;
  });
  return { promise, resolve, reject };
};
const comentario = (id, texto = id, extra = {}) => ({
  id,
  texto,
  publicadoEm: '2026-10-20T12:00:00Z',
  autor: { id: 'u2', nome: 'Bia', avatarUrl: null },
  totalCurtidas: 0,
  curtidoPorMim: false,
  respostas: [],
  ...extra,
});
const comentariosModel = load('features/comentarios/model.ts');

/** Painel com o hook de verdade; a API fica pendente até o teste responder. */
async function mount({ postId = 'p1', visible = true } = {}) {
  const h = { gets: [], posts: [], likes: [], comentados: [], erros: [] };
  const request = (list, args) => {
    const d = deferred();
    list.push({ ...args, ...d });
    return d.promise;
  };
  const api = {
    listarComentarios: (id, signal) => request(h.gets, { id, signal }),
    comentarPost: (id, texto, signal) =>
      request(h.posts, { id, texto, signal }),
  };
  const { useComentarios } = load('features/comentarios/useComentarios.ts', {
    '@/api/client': client,
    '@/features/posts/api': {
      curtir: (id) => request(h.likes, { id, curtir: true }),
      descurtir: (id) => request(h.likes, { id, curtir: false }),
    },
    './api': api,
    './model': comentariosModel,
  });
  const { ComentariosSheet } = load(
    'features/comentarios/ComentariosSheet.tsx',
    {
      'react-native': native,
      '@/theme': theme,
      '@/api/client': client,
      '@/components/avatar': { Avatar: 'Avatar' },
      '@/components/BottomSheet': {
        BottomSheet: ({ children, footer, ...props }) =>
          React.createElement(
            'BottomSheet',
            props,
            children,
            footer && React.createElement('Rodape', null, footer)
          ),
      },
      '@/components/icons/HeartIcon': { HeartIcon: 'HeartIcon' },
      '@/components/icons/SendIcon': { SendIcon: 'SendIcon' },
      '@/components/toast-provider': {
        useToastContext: () => ({
          showErrorToast: (erro) => h.erros.push(erro),
        }),
      },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
      '@/features/forum/model': model,
      '@/features/livro/model': { tempoRelativo: () => 'agora' },
      './model': comentariosModel,
      './useComentarios': { useComentarios },
    }
  );
  h.props = { postId, visible };
  h.render = () =>
    React.createElement(ComentariosSheet, {
      ...h.props,
      onClose: () => {
        h.props = { ...h.props, visible: false };
        h.tree.update(h.render());
      },
      onComentado: (id) => h.comentados.push(id),
    });
  await act(async () => {
    h.tree = create(h.render());
  });
  h.set = (props) =>
    act(async () => {
      h.props = { ...h.props, ...props };
      h.tree.update(h.render());
    });
  h.all = (type) => h.tree.root.findAllByType(type);
  h.textos = () =>
    h.all('Text').flatMap((item) => [].concat(item.props.children));
  h.button = (label) =>
    h.all('PrimaryButton').find((item) => item.props.label === label);
  h.sheet = () => h.tree.root.findByType('BottomSheet');
  h.input = () => h.all('TextInput')[0];
  h.type = (texto) => act(async () => h.input().props.onChangeText(texto));
  h.send = (label = 'Comentar') =>
    act(async () => {
      h.enviar(label).props.onPress();
    });
  h.enviar = (label = 'Comentar') => h.pressable(label);
  h.pressable = (label) =>
    h.all('Pressable').find((item) => item.props.accessibilityLabel === label);
  h.press = (label) =>
    act(async () => {
      h.pressable(label).props.onPress();
    });
  h.dispose = () => act(async () => h.tree.unmount());
  return h;
}

test('API lista e comenta pelo post, com texto original, signal e erro do envelope', async () => {
  const original = global.fetch;
  const calls = [];
  const api = load('features/comentarios/api.ts', { '@/api/client': client });
  const signal = new AbortController().signal;
  const texto = ' \tConcordo\r\n ';
  global.fetch = async (url, init) => {
    calls.push({ url, ...init });
    return init.method === 'POST'
      ? Response.json(comentario('novo', texto), { status: 201 })
      : Response.json({ comentarios: [], podeComentar: true });
  };
  try {
    assert.deepEqual(await api.listarComentarios('post/1', signal), {
      comentarios: [],
      podeComentar: true,
    });
    assert.ok(calls[0].url.endsWith('/api/posts/post%2F1/comentarios'));
    assert.equal(calls[0].signal, signal);
    assert.deepEqual(
      await api.comentarPost('post/1', texto, signal),
      comentario('novo', texto)
    );
    assert.ok(calls[1].url.endsWith('/api/posts/post%2F1/comentarios'));
    assert.equal(calls[1].method, 'POST');
    assert.equal(calls[1].signal, signal);
    assert.deepEqual(JSON.parse(calls[1].body), { texto });
    global.fetch = async () =>
      Response.json(
        {
          code: 'SO_QUEM_SEGUE_COMENTA',
          message: 'Siga a comunidade para comentar.',
        },
        { status: 403 }
      );
    await assert.rejects(
      api.comentarPost('p1', 'oi'),
      (err) =>
        err instanceof client.ApiError &&
        err.code === 'SO_QUEM_SEGUE_COMENTA' &&
        err.message === 'Siga a comunidade para comentar.'
    );
  } finally {
    global.fetch = original;
  }
});

test('texto do comentário: 1 a 1.000 unidades UTF-16 e só espaços não conta', () => {
  for (const texto of ['', ' ', '\t\r\n ', 'a'.repeat(1001)])
    assert.equal(
      model.textoPostValido(texto, 1000),
      false,
      JSON.stringify(texto)
    );
  for (const texto of ['a', 'a'.repeat(1000), '😀'.repeat(500)])
    assert.equal(model.textoPostValido(texto, 1000), true);
  assert.equal(model.textoPostValido('a'.repeat(2000)), true);
});

test('fechado não busca; ao abrir mostra carregando e depois a lista na ordem da API', async () => {
  const h = await mount({ visible: false });
  try {
    assert.equal(h.gets.length, 0);
    await h.set({ visible: true });
    assert.equal(h.gets.length, 1);
    assert.equal(h.gets[0].id, 'p1');
    assert.equal(h.all('ActivityIndicator').length, 1);
    await act(async () =>
      h.gets[0].resolve({
        comentarios: [comentario('antigo'), comentario('novo')],
        podeComentar: true,
      })
    );
    assert.equal(h.all('ActivityIndicator').length, 0);
    const textos = h.textos();
    assert.ok(textos.indexOf('antigo') < textos.indexOf('novo'));
    assert.ok(textos.includes('Bia'));
    assert.ok(textos.includes('agora'));
    assert.equal(h.all('Avatar').length, 2);
  } finally {
    await h.dispose();
  }
});

test('vazio mostra o aviso; quem não segue lê e vê o convite no lugar do campo', async () => {
  for (const podeComentar of [true, false]) {
    const h = await mount();
    try {
      await act(async () =>
        h.gets[0].resolve({
          comentarios: podeComentar ? [] : [comentario('lido')],
          podeComentar,
        })
      );
      const textos = h.textos();
      assert.equal(textos.includes('Nenhum comentário ainda.'), podeComentar);
      assert.equal(textos.includes('lido'), !podeComentar);
      assert.equal(h.all('TextInput').length, podeComentar ? 1 : 0);
      assert.equal(
        textos.includes('Siga a comunidade para comentar.'),
        !podeComentar
      );
    } finally {
      await h.dispose();
    }
  }
});

test('"Comentar" só habilita com texto válido', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ comentarios: [], podeComentar: true })
    );
    assert.equal(h.input().props.placeholder, 'Escreva um comentário...');
    for (const texto of ['', ' \t\r\n ', 'a'.repeat(1001)]) {
      await h.type(texto);
      assert.equal(h.enviar().props.disabled, true);
      await h.send();
    }
    assert.equal(h.posts.length, 0);
    for (const texto of ['a', 'a'.repeat(1000)]) {
      await h.type(texto);
      assert.equal(h.enviar().props.disabled, false);
    }
  } finally {
    await h.dispose();
  }
});

test('envio tem trava; sucesso põe no fim da lista, limpa o campo e avisa a tela', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({
        comentarios: [comentario('antigo')],
        podeComentar: true,
      })
    );
    await h.type(' Meu comentário ');
    await act(async () => {
      const enviar = h.enviar().props.onPress;
      enviar();
      enviar();
    });
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].id, 'p1');
    assert.equal(h.posts[0].texto, ' Meu comentário ');
    assert.equal(h.enviar().props.accessibilityState.busy, true);
    assert.equal(h.input().props.editable, false);
    assert.equal(h.sheet().props.dismissible, false);
    await act(async () => h.sheet().props.onClose());
    assert.equal(h.sheet().props.visible, true);
    await act(async () =>
      h.posts[0].resolve(comentario('meu', ' Meu comentário '))
    );
    const textos = h.textos();
    assert.ok(textos.indexOf('antigo') < textos.indexOf(' Meu comentário '));
    assert.equal(h.input().props.value, '');
    assert.deepEqual(h.comentados, ['p1']);
    assert.equal(h.gets.length, 1);
    assert.equal(h.sheet().props.dismissible, true);
  } finally {
    await h.dispose();
  }
});

test('falha ao comentar mostra o envelope (ou texto amigável) e mantém o texto', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ comentarios: [], podeComentar: true })
    );
    await h.type('Texto que fica');
    await h.send();
    await act(async () =>
      h.posts[0].reject(
        new client.ApiError(403, {
          code: 'SO_QUEM_SEGUE_COMENTA',
          message: 'Siga a comunidade para comentar.',
        })
      )
    );
    const alerta = () =>
      h.tree.root.findByProps({ accessibilityRole: 'alert' }).props.children;
    assert.equal(alerta(), 'Siga a comunidade para comentar.');
    assert.equal(h.input().props.value, 'Texto que fica');
    assert.deepEqual(h.comentados, []);
    await h.send();
    await act(async () =>
      h.posts[1].reject(new TypeError('Network request failed'))
    );
    assert.equal(alerta(), 'Não foi possível comentar. Tente novamente.');
    assert.equal(h.input().props.value, 'Texto que fica');
  } finally {
    await h.dispose();
  }
});

test('erro ao abrir oferece tentar de novo; fechar descarta o texto e reabrir busca de novo', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].reject(
        new client.ApiError(404, {
          code: 'POST_NAO_ENCONTRADO',
          message: 'Post não encontrado.',
        })
      )
    );
    assert.ok(h.textos().includes('Post não encontrado.'));
    await act(async () => h.button('Tentar de novo').props.onPress());
    assert.equal(h.gets.length, 2);
    assert.equal(h.all('ActivityIndicator').length, 1);
    await act(async () =>
      h.gets[1].resolve({ comentarios: [], podeComentar: true })
    );
    await h.type('rascunho');
    await act(async () => h.sheet().props.onClose());
    assert.equal(h.sheet().props.visible, false);
    await h.set({ visible: true });
    assert.equal(h.gets.length, 3);
    await act(async () =>
      h.gets[2].resolve({ comentarios: [], podeComentar: true })
    );
    assert.equal(h.input().props.value, '');
  } finally {
    await h.dispose();
  }
});

test('trocar de post cancela a busca anterior e ignora a resposta antiga', async () => {
  const h = await mount();
  try {
    const antiga = h.gets[0];
    await h.set({ visible: false });
    assert.equal(antiga.signal.aborted, true);
    await h.set({ postId: 'p2', visible: true });
    assert.equal(h.gets[1].id, 'p2');
    await act(async () =>
      antiga.resolve({ comentarios: [comentario('velho')], podeComentar: true })
    );
    assert.equal(h.textos().includes('velho'), false);
    assert.equal(h.all('ActivityIndicator').length, 1);
  } finally {
    await h.dispose();
  }
});

test('PostCard: sem comments não há balão; com handler o balão vira botão', async () => {
  const { PostCard } = load('components/PostCard.tsx', {
    'react-native': native,
    'react-native-svg': { __esModule: true, default: 'Svg', Path: 'Path' },
    '@/theme': { colors: {}, sizes: {} },
    './avatar': { Avatar: 'Avatar', StreakBadge: 'StreakBadge' },
    './Card': { Card: 'Card' },
    './icons/CommentsIcon': { CommentsIcon: 'CommentsIcon' },
    './icons/HeartIcon': { HeartIcon: 'HeartIcon' },
  });
  const base = { authorName: 'Ana', timeAgo: '', text: 'Post', likes: 0 };
  const balao = (tree) =>
    tree.root.findAll(
      (item) =>
        typeof item.type === 'string' &&
        item.props.accessibilityLabel === '3 comentários'
    );
  let tree;
  await act(async () => {
    tree = create(React.createElement(PostCard, base));
  });
  assert.equal(tree.root.findAllByType('CommentsIcon').length, 0);
  await act(async () =>
    tree.update(React.createElement(PostCard, { ...base, comments: 3 }))
  );
  assert.equal(balao(tree)[0].type, 'View');
  let abriu = 0;
  await act(async () =>
    tree.update(
      React.createElement(PostCard, {
        ...base,
        comments: 3,
        onCommentsPress: () => abriu++,
      })
    )
  );
  assert.equal(balao(tree)[0].type, 'Pressable');
  assert.equal(balao(tree)[0].props.accessibilityRole, 'button');
  await act(async () => balao(tree)[0].props.onPress());
  assert.equal(abriu, 1);
  await act(async () => tree.unmount());
});

test('comunidade: somarComentario sobe 1 só no post comentado', async () => {
  const { useComunidade } = load('features/comunidade/useComunidade.ts', {
    '@/api/client': client,
    '@/features/posts/api': {
      curtir: async () => {},
      descurtir: async () => {},
    },
    './api': {
      buscarComunidade: async () => ({
        id: 'c1',
        sigo: true,
        seguidores: 1,
        posts: ['a', 'b'].map((id) => ({
          id,
          totalCurtidas: 0,
          curtidoPorMim: false,
          totalComentarios: 2,
        })),
      }),
      seguirComunidade: async () => ({}),
      deixarDeSeguirComunidade: async () => ({}),
    },
  });
  let hook;
  function Probe() {
    hook = useComunidade('c1');
    return null;
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  await act(async () => hook.somarComentario('a'));
  assert.deepEqual(
    hook.comunidade.dados.posts.map((post) => post.totalComentarios),
    [3, 2]
  );
  await act(async () => tree.unmount());
});

const thread = () => ({
  podeComentar: true,
  comentarios: [
    comentario('c1', 'Primeiro', {
      totalCurtidas: 2,
      respostas: [
        comentario('r1', 'Resposta', {
          respostas: [comentario('r2', 'Resposta da resposta')],
        }),
      ],
    }),
    comentario('c2', 'Segundo'),
  ],
});

test('model: acha, troca e insere em qualquer nível, e conta as respostas', () => {
  const { comentarios } = thread();
  assert.equal(
    comentariosModel.buscarComentario(comentarios, 'r2').texto,
    'Resposta da resposta'
  );
  assert.equal(comentariosModel.buscarComentario(comentarios, 'x'), null);
  assert.equal(comentariosModel.totalDeRespostas(comentarios[0]), 2);
  const trocado = comentariosModel.comComentario(comentarios, 'r2', (c) => ({
    ...c,
    texto: 'trocado',
  }));
  assert.equal(trocado[0].respostas[0].respostas[0].texto, 'trocado');
  assert.equal(
    comentarios[0].respostas[0].respostas[0].texto,
    'Resposta da resposta'
  );
  const comResposta = comentariosModel.inserirNaThread(
    comentarios,
    'r1',
    comentario('n')
  );
  assert.deepEqual(
    comResposta[0].respostas[0].respostas.map((c) => c.id),
    ['r2', 'n']
  );
  assert.deepEqual(
    comentariosModel
      .inserirNaThread(comentarios, null, comentario('n'))
      .map((c) => c.id),
    ['c1', 'c2', 'n']
  );
});

test('thread aparece em árvore, na ordem da API, e as respostas recolhem', async () => {
  const h = await mount();
  try {
    await act(async () => h.gets[0].resolve(thread()));
    const textos = h.textos();
    const ordem = ['Primeiro', 'Resposta', 'Resposta da resposta', 'Segundo'];
    assert.deepEqual(
      ordem.map((t) => textos.indexOf(t)),
      [...ordem.map((t) => textos.indexOf(t))].sort((a, b) => a - b)
    );
    assert.ok(textos.includes('Ocultar respostas'));
    const recolher = h
      .all('Pressable')
      .find(
        (item) =>
          item.findAllByType('Text')[0]?.props.children === 'Ocultar respostas'
      );
    await act(async () => recolher.props.onPress());
    assert.equal(h.textos().includes('Resposta'), false);
    assert.equal(h.textos().includes('Resposta da resposta'), false);
    assert.ok(h.textos().includes('Mostrar 2 respostas'));
    assert.ok(h.textos().includes('Segundo'));
  } finally {
    await h.dispose();
  }
});

test('responder manda para o comentário, põe a resposta abaixo dele e soma no balão', async () => {
  const h = await mount();
  try {
    await act(async () => h.gets[0].resolve(thread()));
    await h.press('Responder a Bia');
    assert.ok(h.textos().includes('Respondendo a '));
    assert.equal(h.input().props.placeholder, 'Escreva uma resposta...');
    assert.equal(h.enviar(), undefined);
    assert.ok(h.enviar('Responder'));
    await h.type('Concordo com você');
    await h.send('Responder');
    assert.equal(h.posts.length, 1);
    assert.equal(h.posts[0].id, 'c1');
    await act(async () =>
      h.posts[0].resolve(comentario('nova', 'Concordo com você'))
    );
    const textos = h.textos();
    assert.ok(
      textos.indexOf('Resposta da resposta') <
        textos.indexOf('Concordo com você')
    );
    assert.ok(textos.indexOf('Concordo com você') < textos.indexOf('Segundo'));
    assert.equal(textos.includes('Respondendo a '), false);
    assert.equal(h.input().props.value, '');
    assert.deepEqual(h.comentados, ['p1']);
    assert.ok(h.enviar());
  } finally {
    await h.dispose();
  }
});

test('cancelar a resposta volta a comentar o post', async () => {
  const h = await mount();
  try {
    await act(async () => h.gets[0].resolve(thread()));
    await h.press('Responder a Bia');
    await h.press('Cancelar resposta');
    await h.type('Comentário no post');
    await h.send();
    assert.equal(h.posts[0].id, 'p1');
  } finally {
    await h.dispose();
  }
});

test('quem não segue não vê "Responder", mas curte e lê a thread', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ ...thread(), podeComentar: false })
    );
    assert.equal(h.pressable('Responder a Bia'), undefined);
    assert.ok(h.pressable('2 curtidas'));
  } finally {
    await h.dispose();
  }
});

test('curtida do comentário é otimista, tem trava e volta ao que era se falhar', async () => {
  const h = await mount();
  try {
    await act(async () => h.gets[0].resolve(thread()));
    await h.press('2 curtidas');
    assert.equal(h.likes.length, 1);
    assert.deepEqual([h.likes[0].id, h.likes[0].curtir], ['c1', true]);
    const curtido = h.pressable('3 curtidas, curtido por você');
    assert.ok(curtido);
    assert.equal(curtido.props.disabled, true);
    await act(async () => curtido.props.onPress());
    assert.equal(h.likes.length, 1);
    await act(async () =>
      h.likes[0].resolve({ curtidoPorMim: true, totalCurtidas: 5 })
    );
    assert.equal(
      h.pressable('5 curtidas, curtido por você').props.disabled,
      false
    );

    await h.press('0 curtidas');
    const resposta = h.likes[1];
    assert.equal(resposta.curtir, true);
    await act(async () =>
      resposta.reject(new TypeError('Network request failed'))
    );
    assert.equal(
      h
        .all('Pressable')
        .filter((p) => p.props.accessibilityLabel === '0 curtidas').length,
      3
    );
    assert.deepEqual(h.erros, [
      'Não foi possível registrar sua curtida. Tente novamente.',
    ]);
  } finally {
    await h.dispose();
  }
});

test('painel: título centralizado, altura mínima e campo de uma linha fixo no rodapé', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ comentarios: [], podeComentar: true })
    );
    const titulo = h
      .all('Text')
      .find((item) => item.props.accessibilityRole === 'header');
    assert.equal(
      [].concat(titulo.props.style).find(Boolean).textAlign,
      'center'
    );
    const container = h
      .all('View')
      .find((item) => [].concat(item.props.style).some((s) => s?.minHeight));
    assert.equal(
      [].concat(container.props.style).find((s) => s?.minHeight).minHeight,
      800 * 0.62
    );
    // Campo e botão ficam no rodapé fixo, fora da lista que rola.
    assert.equal(container.findAllByType('TextInput').length, 0);
    const rodape = h.tree.root.findByType('Rodape');
    assert.equal(rodape.findAllByType('TextInput').length, 1);
    assert.ok(rodape.findAllByType('SendIcon').length === 1);
    const input = [].concat(h.input().props.style).find(Boolean);
    assert.deepEqual([input.minHeight, input.maxHeight], [40, 120]);
  } finally {
    await h.dispose();
  }
});

test('enviar é um botão ao lado do campo, com carregando no lugar do ícone', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ comentarios: [], podeComentar: true })
    );
    assert.equal(h.button('Comentar'), undefined);
    const linha = h.input().parent;
    assert.ok(linha.findAllByType('SendIcon').length === 1);
    assert.equal(h.enviar().parent, linha);
    await h.type('Oi');
    await h.send();
    assert.equal(h.all('SendIcon').length, 0);
    assert.equal(h.enviar().findAllByType('ActivityIndicator').length, 1);
  } finally {
    await h.dispose();
  }
});

test('recolher e expandir todas as respostas de uma vez', async () => {
  const h = await mount();
  try {
    await act(async () => h.gets[0].resolve(thread()));
    const geral = () =>
      h
        .all('Pressable')
        .find((item) =>
          ['Recolher respostas', 'Expandir respostas'].includes(
            [].concat(item.findByType('Text').props.children)[0]
          )
        );
    const rotulo = () => geral().findByType('Text').props.children;
    assert.equal(rotulo(), 'Recolher respostas');
    await act(async () => geral().props.onPress());
    assert.equal(rotulo(), 'Expandir respostas');
    assert.equal(h.textos().includes('Resposta'), false);
    assert.ok(h.textos().includes('Mostrar 2 respostas'));
    await act(async () => geral().props.onPress());
    assert.equal(rotulo(), 'Recolher respostas');
    assert.ok(h.textos().includes('Resposta da resposta'));
  } finally {
    await h.dispose();
  }
});

test('sem respostas na thread, o controle geral não aparece', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({
        comentarios: [comentario('so')],
        podeComentar: true,
      })
    );
    assert.equal(h.textos().includes('Recolher respostas'), false);
  } finally {
    await h.dispose();
  }
});

test('quem não segue vê o convite no rodapé; carregando e erro não têm rodapé', async () => {
  const h = await mount();
  try {
    assert.equal(h.tree.root.findAllByType('Rodape').length, 0);
    await act(async () =>
      h.gets[0].resolve({ comentarios: [], podeComentar: false })
    );
    const rodape = h.tree.root.findByType('Rodape');
    assert.ok(
      rodape
        .findAllByType('Text')
        .some((t) => t.props.children === 'Siga a comunidade para comentar.')
    );
  } finally {
    await h.dispose();
  }
});

// Revisão do PR #211: reabrir reaproveitava a chave da abertura anterior. O painel mostrava a
// lista velha com o campo liberado, e a busca atrasada apagava o comentário recém-criado.
test('reabrir mostra carregando sem campo; comentário feito depois não some com a busca', async () => {
  const h = await mount();
  try {
    await act(async () =>
      h.gets[0].resolve({ comentarios: [comentario('c1')], podeComentar: true })
    );
    await act(async () => h.sheet().props.onClose());
    await h.set({ visible: true });
    assert.equal(h.gets.length, 2);
    assert.equal(h.all('ActivityIndicator').length, 1);
    assert.equal(h.all('TextInput').length, 0);
    assert.equal(h.textos().includes('c1'), false);

    await act(async () =>
      h.gets[1].resolve({ comentarios: [comentario('c1')], podeComentar: true })
    );
    await h.type('Novo');
    await h.send();
    await act(async () => h.posts[0].resolve(comentario('novo', 'Novo')));
    assert.ok(h.textos().includes('Novo'));
    assert.deepEqual(h.comentados, ['p1']);
  } finally {
    await h.dispose();
  }
});

test('resposta atrasada de uma abertura anterior não sobrescreve a atual', async () => {
  const h = await mount();
  try {
    const primeira = h.gets[0];
    await act(async () => h.sheet().props.onClose());
    await h.set({ visible: true });
    await act(async () =>
      h.gets[1].resolve({
        comentarios: [comentario('atual')],
        podeComentar: true,
      })
    );
    await act(async () =>
      primeira.resolve({
        comentarios: [comentario('velha')],
        podeComentar: true,
      })
    );
    assert.ok(h.textos().includes('atual'));
    assert.equal(h.textos().includes('velha'), false);
  } finally {
    await h.dispose();
  }
});

// Revisão do PR #211: a falha da curtida restaurava o post inteiro e desfazia o balão.
test('comunidade: falha da curtida não desfaz o comentário somado no meio tempo', async () => {
  let curtida;
  const { useComunidade } = load('features/comunidade/useComunidade.ts', {
    '@/api/client': client,
    '@/features/posts/api': {
      curtir: () =>
        new Promise((resolve, reject) => {
          curtida = { resolve, reject };
        }),
      descurtir: async () => {},
    },
    './api': {
      buscarComunidade: async () => ({
        id: 'c1',
        sigo: true,
        seguidores: 1,
        posts: [
          {
            id: 'a',
            totalCurtidas: 4,
            curtidoPorMim: false,
            totalComentarios: 3,
          },
        ],
      }),
      seguirComunidade: async () => ({}),
      deixarDeSeguirComunidade: async () => ({}),
    },
  });
  let hook;
  function Probe() {
    hook = useComunidade('c1');
    return null;
  }
  let tree;
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  let pendente;
  await act(async () => {
    pendente = hook.alternarCurtida('a').catch((erro) => erro);
  });
  assert.equal(hook.comunidade.dados.posts[0].totalCurtidas, 5);
  await act(async () => hook.somarComentario('a'));
  await act(async () => {
    curtida.reject(new Error('falhou'));
    await pendente;
  });
  assert.deepEqual(hook.comunidade.dados.posts[0], {
    id: 'a',
    totalCurtidas: 4,
    curtidoPorMim: false,
    totalComentarios: 4,
  });
  await act(async () => tree.unmount());
});
