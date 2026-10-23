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

const payload = (mensal = {}, semanal = {}) => ({
  mensal: {
    titulo: 'Desafio da leitura mensal',
    descricao: 'Leia 2 livros este mês',
    meta: 2,
    progresso: 0,
    unidade: 'livros',
    diasRestantes: 9,
    xp: 200,
    cumprido: false,
    ...mensal,
  },
  semanal: {
    titulo: null,
    descricao: 'Leia 70 páginas essa semana',
    meta: 70,
    progresso: 0,
    unidade: 'paginas',
    diasRestantes: 3,
    xp: 100,
    cumprido: false,
    ...semanal,
  },
});

async function setup() {
  const sistema = [];
  const amigos = [];
  const focus = new Set();
  const pending = (queue, extra) =>
    new Promise((resolve, reject) => queue.push({ ...extra, resolve, reject }));
  const native = {
    View: 'View',
    Text: 'Text',
    ScrollView: 'ScrollView',
    ActivityIndicator: 'ActivityIndicator',
    StyleSheet: { create: (s) => s },
  };
  const theme = { ...require('../src/theme/tokens'), textStyles: {} };
  class ApiError extends Error {}
  const router = {
    useRouter: () => ({ push: () => {} }),
    useFocusEffect: (callback) =>
      React.useEffect(() => {
        const entry = { callback, cleanup: callback() };
        focus.add(entry);
        return () => {
          entry.cleanup?.();
          focus.delete(entry);
        };
      }, [callback]),
  };
  const api = load('features/desafios/sistemaApi.ts', {
    '@/api/client': {
      apiGet: (url, options) =>
        pending(sistema, { url, signal: options.signal }),
    },
  });
  const hook = load('features/desafios/useDesafiosSistema.ts', {
    'expo-router': router,
    '@/api/client': { ApiError },
    './sistemaApi': api,
  });
  const deps = {
    'react-native': native,
    'expo-router': router,
    '@/theme': theme,
    '@/api/client': { ApiError },
    './useDesafiosSistema': hook,
  };
  for (const name of ['Card', 'PrimaryButton', 'AppHeader', 'EmptyState'])
    deps[`@/components/${name}`] = { [name]: name };
  deps['@/components/avatar'] = { Avatar: 'Avatar' };
  for (const name of [
    'ClockIcon',
    'TargetIcon',
    'TrophyIcon',
    'UsersIcon',
    'RematchIcon',
    'SadFaceIcon',
  ])
    deps[`@/components/icons/${name}`] = { [name]: name };
  deps['@/components/ProgressBar'] = load('components/ProgressBar.tsx', deps);
  const sections = load('features/desafios/DesafiosSistema.tsx', deps);
  const { DesafiosScreen } = load('features/desafios/DesafioScreen.tsx', {
    ...deps,
    './DesafiosSistema': sections,
    './api': { listarTodosOsDesafios: (signal) => pending(amigos, { signal }) },
  });
  let tree;
  await act(async () => {
    tree = create(React.createElement(DesafiosScreen));
  });
  return {
    tree,
    sistema,
    amigos,
    ApiError,
    text: () =>
      tree.root
        .findAllByType('Text')
        .map((n) => n.children.filter((x) => typeof x === 'string').join(''))
        .join('|'),
    button: (label) =>
      tree.root
        .findAllByType('PrimaryButton')
        .find((n) => n.props.label === label),
    bars: () =>
      tree.root
        .findAllByType('View')
        .flatMap((n) =>
          Array.isArray(n.props.style)
            ? n.props.style
                .filter((s) => s?.width?.endsWith?.('%'))
                .map((s) => s.width)
            : []
        ),
    blur: () =>
      act(() => {
        for (const entry of focus) {
          entry.cleanup?.();
          entry.cleanup = null;
        }
      }),
    refocus: () =>
      act(() => {
        for (const entry of focus) entry.cleanup = entry.callback();
      }),
    close: () => act(() => tree.unmount()),
  };
}

test('API usa apiGet, contrato real e encaminha AbortSignal', async () => {
  const calls = [];
  const response = payload();
  const api = load('features/desafios/sistemaApi.ts', {
    '@/api/client': {
      apiGet: async (...args) => {
        calls.push(args);
        return response;
      },
    },
  });
  const controller = new AbortController();
  assert.equal(await api.consultarDesafiosSistema(controller.signal), response);
  assert.deepEqual(calls, [
    ['/api/desafios/sistema', { signal: controller.signal }],
  ]);
  assert.equal(await api.consultarDesafiosSistema(), response);
  assert.equal(calls[1][1].signal, undefined);
});

test('seções em ordem, zero, loading independente, cumprimento e barras limitadas', async () => {
  const h = await setup();
  try {
    assert.equal(h.sistema.length, 1);
    assert.equal(h.amigos.length, 1);
    assert.equal(h.sistema[0].url, '/api/desafios/sistema');
    await act(async () => h.amigos[0].resolve([]));
    assert.equal(h.tree.root.findAllByType('EmptyState').length, 1);
    assert.equal(h.tree.root.findAllByType('ActivityIndicator').length, 2);
    await act(async () => h.sistema[0].resolve(payload()));
    const text = h.text();
    assert.ok(text.indexOf('Desafio ativo') < text.indexOf('Desafio semanal'));
    assert.ok(
      text.indexOf('Desafio semanal') < text.indexOf('Desafio com amigos')
    );
    for (const value of [
      'Desafio da leitura mensal',
      'Leia 2 livros este mês',
      'Leia 70 páginas essa semana',
      '0/2 livros',
      '0/70 páginas',
      '+ 200 XP',
      '+ 100 XP',
      '9 dias restantes',
    ])
      assert.ok(text.includes(value), value);
    assert.ok(!text.includes('3 dias restantes'));
    assert.deepEqual(h.bars(), ['0%', '0%']);
    h.blur();
    h.refocus();
    assert.ok(!h.text().includes('0/2 livros'));
    await act(async () =>
      h.sistema[1].resolve(
        payload(
          { progresso: 2, cumprido: true },
          { progresso: 70, cumprido: true }
        )
      )
    );
    assert.equal(h.text().split('Desafio cumprido!').length - 1, 2);
    assert.ok(!h.text().includes('dias restantes'));
    assert.deepEqual(h.bars(), ['100%', '100%']);
    h.blur();
    h.refocus();
    await act(async () =>
      h.sistema[2].resolve(
        payload(
          { progresso: -1, cumprido: false },
          { progresso: 200, cumprido: false }
        )
      )
    );
    assert.deepEqual(h.bars(), ['0%', '100%']);
    assert.ok(!h.text().includes('Desafio cumprido!')); // cumprimento pertence ao backend
  } finally {
    h.close();
  }
});

test('erro nas duas seções, danger, retry isolado e atualização falha sem dados antigos', async () => {
  const h = await setup();
  try {
    await act(async () => h.amigos[0].resolve([]));
    await act(async () =>
      h.sistema[0].reject(new h.ApiError('Sistema indisponível'))
    );
    const errors = h.tree.root
      .findAllByType('Text')
      .filter((n) => n.props.accessibilityRole === 'alert');
    assert.equal(errors.length, 2);
    for (const error of errors)
      assert.equal(
        error.props.style.color,
        require('../src/theme/tokens').colors.danger
      );
    assert.equal(h.text().split('Sistema indisponível').length - 1, 2);
    assert.ok(h.button('Desafiar Amigo'));
    assert.equal(h.tree.root.findAllByType('EmptyState').length, 1);
    await act(async () => {
      h.button('Tentar novamente').props.onPress();
    });
    assert.equal(h.sistema.length, 2);
    assert.equal(h.amigos.length, 1);
    await act(async () => h.sistema[1].resolve(payload()));
    h.blur();
    h.refocus();
    await act(async () => h.sistema[2].reject(new Error('offline')));
    assert.equal(
      h.text().split('Não foi possível carregar os desafios do sistema.')
        .length - 1,
      2
    );
    assert.ok(!h.text().includes('Leia 2 livros'));
    assert.equal(h.tree.root.findAllByType('EmptyState').length, 1);
  } finally {
    h.close();
  }
});

test('sistema funciona com amigos lento ou em erro; retry de amigos não consulta sistema', async () => {
  const h = await setup();
  try {
    await act(async () =>
      h.sistema[0].resolve(payload({ progresso: 1 }, { progresso: 35 }))
    );
    assert.deepEqual(h.bars(), ['50%', '50%']);
    assert.equal(h.tree.root.findAllByType('ActivityIndicator').length, 1);
    await act(async () => h.amigos[0].reject(new Error('offline')));
    assert.ok(h.text().includes('1/2 livros'));
    await act(async () => {
      h.button('Tentar de novo').props.onPress();
    });
    assert.equal(h.sistema.length, 1);
    assert.equal(h.amigos.length, 2);
  } finally {
    h.close();
  }
});

test('foco recarrega; blur/unmount cancelam; respostas e erros antigos são ignorados', async () => {
  const h = await setup();
  h.blur();
  assert.equal(h.sistema[0].signal.aborted, true);
  h.refocus();
  assert.equal(h.sistema.length, 2);
  await act(async () => h.sistema[1].resolve(payload({ progresso: 1 })));
  await act(async () => h.sistema[0].resolve(payload({ progresso: 2 })));
  assert.ok(h.text().includes('1/2 livros'));
  h.blur();
  h.refocus();
  const old = h.sistema[2];
  h.blur();
  h.refocus();
  await act(async () => h.sistema[3].resolve(payload()));
  await act(async () => old.reject(new Error('erro antigo')));
  assert.ok(h.text().includes('0/2 livros'));
  assert.ok(!h.button('Tentar novamente'));
  h.blur();
  h.refocus();
  h.close();
  assert.equal(h.sistema[4].signal.aborted, true);
  await act(async () => h.sistema[4].resolve(payload()));
});
