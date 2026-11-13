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
        target: ts.ScriptTarget.ES2022,
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
const validation = load('features/comunidade/criacao.ts');
const client = load('api/client.ts');

async function mount() {
  const h = { requests: [], saved: [], closes: 0 };
  const { CriarComunidadeSheet } = load(
    'features/comunidade/CriarComunidadeSheet.tsx',
    {
      'react-native': {
        View: 'View',
        Text: 'Text',
        Pressable: 'Pressable',
        TextInput: 'TextInput',
        StyleSheet: { create: (s) => s },
      },
      'expo-image': { Image: 'Image' },
      'expo-linear-gradient': { LinearGradient: 'LinearGradient' },
      '@/api/client': client,
      '@/components/BottomSheet': {
        BottomSheet: ({ footer, children, ...props }) =>
          React.createElement('BottomSheet', props, children, footer),
      },
      '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
      '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
      './criacao': validation,
      './api': {
        criarComunidade: (body, signal) =>
          new Promise((resolve, reject) =>
            h.requests.push({ body, signal, resolve, reject })
          ),
      },
    }
  );
  const props = {
    visible: true,
    onClose: () => h.closes++,
    onSuccess: (c) => h.saved.push(c),
  };
  await act(async () => {
    h.tree = create(React.createElement(CriarComunidadeSheet, props));
  });
  h.input = (label) => h.tree.root.findByProps({ accessibilityLabel: label });
  h.submit = () => h.tree.root.findByType('PrimaryButton');
  h.type = (label, value) =>
    act(async () => h.input(label).props.onChangeText(value));
  h.visible = (visible) =>
    act(async () =>
      h.tree.update(
        React.createElement(CriarComunidadeSheet, { ...props, visible })
      )
    );
  h.dispose = () => act(async () => h.tree.unmount());
  return h;
}

test('validação rejeita nomes vazios, controles, limites e URLs inseguras', () => {
  for (const nome of ['', '  ', 'a'.repeat(121), 'Livro\nOutro'])
    assert.ok(validation.erroNomeComunidade(nome));
  assert.equal(validation.erroNomeComunidade('a'.repeat(120)), null);
  for (const url of [
    'http://example.com/a.png',
    'data:image/png;base64,aa',
    'javascript:alert(1)',
    'https://user:pass@example.com/a.png',
    'https:///capa',
    'https://example.com:99999/a.png',
    'https://example.com/' + 'a'.repeat(2048),
  ])
    assert.ok(validation.erroImagemComunidade(url), url);
  assert.equal(validation.erroImagemComunidade(''), null);
  assert.equal(
    validation.erroImagemComunidade(' https://example.com/a.png '),
    null
  );
});

test('envio único bloqueia fechamento e edições; falha preserva campos e permite retry', async () => {
  const h = await mount();
  try {
    assert.equal(h.submit().props.disabled, true);
    await h.type('Nome da comunidade', '  Leitores  ');
    await h.type('Descrição da comunidade', '  Boas histórias.  ');
    await act(async () => {
      const submit = h.submit().props.onPress;
      submit();
      submit();
      h.tree.root.findByType('BottomSheet').props.onClose();
      h.input('Nome da comunidade').props.onChangeText('Outro nome');
    });
    assert.equal(h.requests.length, 1);
    assert.equal(h.closes, 0);
    assert.equal(
      h.tree.root.findByType('BottomSheet').props.dismissible,
      false
    );
    assert.equal(h.input('Nome da comunidade').props.value, '  Leitores  ');
    assert.deepEqual(h.requests[0].body, {
      nome: 'Leitores',
      descricao: 'Boas histórias.',
      imagemUrl: null,
    });
    await act(async () =>
      h.requests[0].reject(
        new client.ApiError(503, {
          code: 'UNAVAILABLE',
          message: 'Tente novamente.',
        })
      )
    );
    assert.equal(
      h.tree.root.findByProps({ accessibilityRole: 'alert' }).props.children,
      'Tente novamente.'
    );
    assert.equal(
      h.input('Descrição da comunidade').props.value,
      '  Boas histórias.  '
    );
    await act(async () => {
      h.submit().props.onPress();
    });
    await act(async () =>
      h.requests[1].resolve({
        id: 'nova',
        nome: 'Leitores',
        sigo: true,
        seguidores: 1,
      })
    );
    assert.equal(h.saved.length, 1);
  } finally {
    await h.dispose();
  }
});

test('fechar ou desmontar cancela envio; resposta antiga não interfere em nova abertura', async () => {
  const h = await mount();
  let desmontado = false;
  try {
    await h.type('Nome da comunidade', 'Primeira');
    await act(async () => {
      h.submit().props.onPress();
    });
    await h.visible(false);
    assert.equal(h.requests[0].signal.aborted, true);
    await h.visible(true);
    await h.type('Nome da comunidade', 'Segunda');
    await act(async () => {
      h.submit().props.onPress();
    });
    await act(async () => h.requests[0].resolve({ id: 'velha' }));
    assert.equal(h.saved.length, 0);
    assert.equal(h.submit().props.loading, true);
    await h.dispose();
    desmontado = true;
    assert.equal(h.requests[1].signal.aborted, true);
    await act(async () => h.requests[1].resolve({ id: 'segunda' }));
    assert.equal(h.saved.length, 0);
  } finally {
    if (!desmontado) await h.dispose();
  }
});

test('API envia corpo, token e signal ao endpoint de criação', async () => {
  const api = load('features/comunidade/api.ts', { '@/api/client': client });
  const original = global.fetch;
  let call;
  client.configurarAutenticacao({
    obterToken: async () => 'token',
    renovarToken: async () => null,
    aoPerderSessao: async () => {},
  });
  global.fetch = async (url, options) => {
    call = { url, ...options };
    return Response.json({ id: 'nova' }, { status: 201 });
  };
  try {
    const controller = new AbortController();
    const body = { nome: 'Leitores', descricao: null, imagemUrl: null };
    assert.deepEqual(await api.criarComunidade(body, controller.signal), {
      id: 'nova',
    });
    assert.ok(call.url.endsWith('/api/comunidades'));
    assert.equal(call.method, 'POST');
    assert.equal(
      new Headers(call.headers).get('Authorization'),
      'Bearer token'
    );
    assert.equal(call.signal, controller.signal);
    assert.deepEqual(JSON.parse(call.body), body);
  } finally {
    global.fetch = original;
    client.configurarAutenticacao(null);
  }
});
