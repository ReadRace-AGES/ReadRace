const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.ReactJSX,
    },
  });
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => dependencies[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}

const native = {
  View: 'View',
  Text: 'Text',
  TextInput: 'TextInput',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (v) => v },
};
const theme = {
  bookCover: { detail: { height: 120 } },
  colors: {},
  spacing: {},
  sizes: {},
  radius: {},
  textStyles: {},
};
const { ApiError } = load('src/api/client.ts');
const { ProgressoTimeoutError } = load('src/features/progresso/api.ts', {
  '@/api/client': {},
});

test('detalhe abre modal, valida, bloqueia toque duplo, preserva erro e aplica sucesso sem novo GET', async () => {
  let resolvePost, rejectPost;
  let posts = 0,
    gets = 0;
  const progressoApi = {
    ProgressoTimeoutError,
    registrarProgresso: () => {
      posts++;
      return new Promise((resolve, reject) => {
        resolvePost = resolve;
        rejectPost = reject;
      });
    },
  };
  const { RegistrarProgressoSheet } = load(
    'src/features/progresso/RegistrarProgressoSheet.tsx',
    {
      'react-native': native,
      'react-native-svg': { __esModule: true, default: 'Svg', Path: 'Path' },
      '@/theme': theme,
      '@/api/client': { ApiError },
      './api': progressoApi,
      '@/components/BottomSheet': {
        BottomSheet: (props) =>
          props.visible
            ? React.createElement('BottomSheet', props, props.children)
            : null,
      },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    }
  );
  const model = load('src/features/livro/model.ts');
  const { useLivroDetalhe } = load('src/features/livro/useLivroDetalhe.ts', {
    '@/api/client': { ApiError },
    '@/features/posts/api': {
      curtir: async () => ({ curtidoPorMim: true, totalCurtidas: 1 }),
      descurtir: async () => ({ curtidoPorMim: false, totalCurtidas: 0 }),
    },
    './model': model,
    './api': {
      buscarDetalhe: async () => {
        gets++;
        return {
          livro: { id: '1', titulo: 'Livro', totalPaginas: 200 },
          progresso: null,
          posts: [],
        };
      },
    },
  });
  const dependencies = {
    'react-native': native,
    '@/theme': theme,
    './model': model,
    './useLivroDetalhe': { useLivroDetalhe },
    'expo-router': {
      useRouter: () => ({}),
      useFocusEffect: (callback) => React.useEffect(callback, [callback]),
    },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showToast: () => {},
        showErrorToast: () => {},
      }),
    },
    '@/features/progresso/RegistrarProgressoSheet': { RegistrarProgressoSheet },
  };
  for (const name of [
    'AppHeader',
    'BookCover',
    'Card',
    'EmptyState',
    'PostCard',
    'PrimaryButton',
    'ProgressBar',
  ])
    dependencies[`@/components/${name}`] = { [name]: name };
  const { LivroDetalheScreen } = load(
    'src/features/livro/LivroDetalheScreen.tsx',
    dependencies
  );
  let renderer;
  await act(async () => {
    renderer = create(
      React.createElement(LivroDetalheScreen, { livroId: '1' })
    );
  });
  try {
    const button = (label) =>
      renderer.root
        .findAllByType('PrimaryButton')
        .find((n) => n.props.label === label);
    await act(async () =>
      renderer.root
        .findAllByType('Pressable')
        .find(
          (n) => n.props.accessibilityLabel === 'Registrar progresso de leitura'
        )
        .props.onPress()
    );
    assert.equal(button('Atualizar Progresso').props.disabled, true);
    for (const value of ['0', '-1', '201', '1.5']) {
      await act(async () =>
        renderer.root.findByType('TextInput').props.onChangeText(value)
      );
      assert.equal(button('Atualizar Progresso').props.disabled, true);
    }
    await act(async () =>
      renderer.root.findByType('TextInput').props.onChangeText('145')
    );
    let pending;
    await act(async () => {
      const submit = button('Atualizar Progresso').props.onPress;
      pending = submit();
      void submit();
    });
    assert.equal(posts, 1);
    assert.equal(
      renderer.root.findByType('BottomSheet').props.dismissible,
      false
    );
    assert.equal(renderer.root.findByType('TextInput').props.editable, false);
    assert.equal(button('Cancelar').props.disabled, true);
    await act(async () => {
      rejectPost(
        new ApiError(422, {
          code: 'PAGINA_INVALIDA',
          message: 'Página inválida',
        })
      );
      await pending;
    });
    assert.equal(renderer.root.findByType('TextInput').props.value, '145');
    assert.ok(
      renderer.root
        .findAllByType('Text')
        .some((n) => n.props.children === 'Página inválida')
    );
    assert.equal(renderer.root.findByType('ProgressBar').props.progress, 0);
    await act(async () => {
      pending = button('Atualizar Progresso').props.onPress();
    });
    await act(async () => {
      resolvePost({
        paginaAtual: 145,
        paginaMaximaAlcancada: 160,
        totalPaginas: 200,
        percentual: 73,
        concluido: false,
        xpTotal: 0,
      });
      await pending;
    });
    assert.equal(renderer.root.findAllByType('BottomSheet').length, 0);
    assert.equal(renderer.root.findByType('ProgressBar').props.progress, 73);
    assert.equal(gets, 1);
    await act(async () =>
      renderer.root
        .findAllByType('Pressable')
        .find(
          (n) =>
            n.props.accessibilityLabel ===
            'Seu progresso. Registrar progresso de leitura'
        )
        .props.onPress()
    );
    assert.equal(renderer.root.findByType('TextInput').props.value, '');
    await act(async () =>
      renderer.root.findByType('TextInput').props.onChangeText('150')
    );
    await act(async () => {
      pending = button('Atualizar Progresso').props.onPress();
    });
    await act(async () => {
      rejectPost(new ProgressoTimeoutError());
      await pending;
    });
    assert.equal(
      renderer.root.findByType('BottomSheet').props.dismissible,
      true
    );
    assert.equal(renderer.root.findByType('TextInput').props.value, '150');
    assert.ok(
      renderer.root
        .findAllByType('Text')
        .some(
          (n) =>
            typeof n.props.children === 'string' &&
            n.props.children.includes('antes de tentar novamente')
        )
    );
    await act(async () => button('Cancelar').props.onPress());
    assert.equal(renderer.root.findAllByType('BottomSheet').length, 0);
  } finally {
    await act(async () => renderer.unmount());
  }
});

test('POST expira, aborta a chamada e não repete o registro automaticamente', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  let calls = 0,
    signal;
  const { registrarProgresso, ProgressoTimeoutError } = load(
    'src/features/progresso/api.ts',
    {
      '@/api/client': {
        apiRequest: (url, init) => {
          calls++;
          signal = init.signal;
          assert.equal(url, '/api/livros/livro%2F1/progresso');
          assert.equal(init.body, '{"pagina":145}');
          return new Promise(() => {});
        },
      },
    }
  );
  const result = assert.rejects(
    registrarProgresso('livro/1', 145),
    ProgressoTimeoutError
  );
  t.mock.timers.tick(15000);
  await result;
  assert.equal(signal.aborted, true);
  assert.equal(calls, 1);
});
