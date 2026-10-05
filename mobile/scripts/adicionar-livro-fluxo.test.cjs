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
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (v) => v },
};
const theme = {
  colors: {},
  spacing: {},
  radius: {},
  sizes: {},
  textStyles: {},
  bookCover: {},
};

const livroCatalogo = {
  volumeId: 'br001',
  titulo: 'Dom Casmurro',
  autor: 'Machado de Assis',
  capaUrl: 'https://capa.jpg',
  isbn: '9786586490077',
  paginas: 256,
};

class ApiError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ApiError';
  }
}

function montarSheet(postsApi) {
  return load('src/features/adicionarLivro/SelecionarListaSheet.tsx', {
    'react-native': native,
    '@/theme': theme,
    '@/api/client': { ApiError },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/BookCover': { BookCover: 'BookCover' },
    '@/components/BottomSheet': {
      BottomSheet: (props) =>
        props.visible
          ? React.createElement('BottomSheet', props, props.children)
          : null,
    },
    './api': postsApi,
  });
}

function montarTela({ buscaInicial, postsApi, routerMocks = {} }) {
  const buscaState = { atual: buscaInicial };
  const { SelecionarListaSheet } = montarSheet(postsApi);

  const dependencies = {
    'react-native': native,
    '@/theme': theme,
    'expo-router': {
      useRouter: () => ({
        canGoBack: () => true,
        back: routerMocks.back ?? (() => {}),
        replace: routerMocks.replace ?? (() => {}),
        push: routerMocks.push ?? (() => {}),
        dismissTo: routerMocks.dismissTo ?? (() => {}),
      }),
    },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/BookCover': { BookCover: 'BookCover' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/SearchInput': { SearchInput: 'SearchInput' },
    './SelecionarListaSheet': { SelecionarListaSheet },
    './useBuscaCatalogo': {
      useBuscaCatalogo: () => ({
        busca: buscaState.atual,
        recarregar: () => {},
      }),
    },
  };

  const { AdicionarLivroScreen } = load(
    'src/features/adicionarLivro/AdicionarLivroScreen.tsx',
    dependencies
  );

  return { AdicionarLivroScreen, buscaState };
}

function achar(renderer, type, matcher) {
  return renderer.root
    .findAllByType(type)
    .find((n) => (matcher ? matcher(n) : true));
}

test('tela de busca: carregando, erro, vazio e lista de resultados', async () => {
  const { AdicionarLivroScreen, buscaState } = montarTela({
    buscaInicial: { situacao: 'inicial' },
    postsApi: { adicionarNaBiblioteca: async () => {} },
  });

  let renderer;
  await act(async () => {
    renderer = create(React.createElement(AdicionarLivroScreen));
  });
  try {
    assert.equal(achar(renderer, 'AppHeader').props.title, 'Adicionar Livro');
    assert.equal(renderer.root.findAllByType('ListItem').length, 0);
    assert.equal(renderer.root.findAllByType('EmptyState').length, 0);

    buscaState.atual = { situacao: 'carregando' };
    await act(async () =>
      renderer.update(React.createElement(AdicionarLivroScreen))
    );
    assert.equal(renderer.root.findAllByType('ActivityIndicator').length, 1);

    buscaState.atual = { situacao: 'erro', mensagem: 'Falha na busca' };
    await act(async () =>
      renderer.update(React.createElement(AdicionarLivroScreen))
    );
    assert.equal(achar(renderer, 'EmptyState').props.message, 'Falha na busca');

    buscaState.atual = { situacao: 'sucesso', resultados: [] };
    await act(async () =>
      renderer.update(React.createElement(AdicionarLivroScreen))
    );
    assert.equal(
      achar(renderer, 'EmptyState').props.message,
      'Nenhum livro encontrado.'
    );

    buscaState.atual = { situacao: 'sucesso', resultados: [livroCatalogo] };
    await act(async () =>
      renderer.update(React.createElement(AdicionarLivroScreen))
    );
    const item = achar(renderer, 'ListItem');
    assert.equal(item.props.title, 'Dom Casmurro');
    assert.equal(item.props.subtitle, 'Machado de Assis');
  } finally {
    await act(async () => renderer.unmount());
  }
});

test('tocar num resultado abre o modal; escolher uma lista leva à confirmação', async () => {
  let chamadas = 0;
  const postsApi = {
    adicionarNaBiblioteca: async (volumeId, lista) => {
      chamadas++;
      assert.equal(volumeId, 'br001');
      assert.equal(lista, 'desejo');
      return {
        livroId: 'uuid-1',
        titulo: 'Dom Casmurro',
        capaUrl: 'https://capa.jpg',
        status: 'desejo',
        favorito: false,
      };
    },
  };
  const { AdicionarLivroScreen } = montarTela({
    buscaInicial: { situacao: 'sucesso', resultados: [livroCatalogo] },
    postsApi,
  });

  let renderer;
  await act(async () => {
    renderer = create(React.createElement(AdicionarLivroScreen));
  });
  try {
    assert.equal(renderer.root.findAllByType('BottomSheet').length, 0);

    await act(async () => achar(renderer, 'ListItem').props.onPress());
    const sheet = achar(renderer, 'BottomSheet');
    assert.ok(sheet);
    assert.equal(
      renderer.root
        .findAllByType('Text')
        .some((n) => n.props.children === 'Dom Casmurro'),
      true
    );

    const botaoDesejos = achar(
      renderer,
      'PrimaryButton',
      (n) => n.props.label === 'Desejos'
    );
    await act(async () => botaoDesejos.props.onPress());

    assert.equal(chamadas, 1);
    assert.equal(renderer.root.findAllByType('BottomSheet').length, 0);
    assert.equal(
      renderer.root
        .findAllByType('Text')
        .some((n) => n.props.children === 'Livro adicionado com sucesso!'),
      true
    );
    assert.equal(
      achar(
        renderer,
        'PrimaryButton',
        (n) => n.props.label === 'Adicionar novo livro'
      ) != null,
      true
    );
  } finally {
    await act(async () => renderer.unmount());
  }
});

test('falha ao gravar: mostra erro e mantém o modal aberto, sem travar novo toque', async () => {
  let tentativas = 0;
  const postsApi = {
    adicionarNaBiblioteca: async () => {
      tentativas++;
      if (tentativas === 1) throw new ApiError('Falha ao gravar');
      return {
        livroId: 'uuid-1',
        titulo: 'Dom Casmurro',
        capaUrl: null,
        status: 'lido',
        favorito: false,
      };
    },
  };
  const { AdicionarLivroScreen } = montarTela({
    buscaInicial: { situacao: 'sucesso', resultados: [livroCatalogo] },
    postsApi,
  });

  let renderer;
  await act(async () => {
    renderer = create(React.createElement(AdicionarLivroScreen));
  });
  try {
    await act(async () => achar(renderer, 'ListItem').props.onPress());
    const botaoLido = achar(
      renderer,
      'PrimaryButton',
      (n) => n.props.label === 'Lido'
    );
    await act(async () => botaoLido.props.onPress());

    assert.equal(renderer.root.findAllByType('BottomSheet').length, 1);
    assert.equal(
      renderer.root
        .findAllByType('Text')
        .some((n) => n.props.children === 'Falha ao gravar'),
      true
    );
    assert.equal(
      achar(renderer, 'PrimaryButton', (n) => n.props.label === 'Lido').props
        .loading,
      false
    );

    await act(async () =>
      achar(
        renderer,
        'PrimaryButton',
        (n) => n.props.label === 'Lido'
      ).props.onPress()
    );
    assert.equal(tentativas, 2);
    assert.equal(renderer.root.findAllByType('BottomSheet').length, 0);
  } finally {
    await act(async () => renderer.unmount());
  }
});

test('"Adicionar novo livro" volta para a busca vazia; "Fechar" navega com o sinal de atualização', async () => {
  let destinoFechar;
  const postsApi = {
    adicionarNaBiblioteca: async () => ({
      livroId: 'uuid-1',
      titulo: 'Dom Casmurro',
      capaUrl: null,
      status: 'desejo',
      favorito: false,
    }),
  };
  const { AdicionarLivroScreen } = montarTela({
    buscaInicial: { situacao: 'sucesso', resultados: [livroCatalogo] },
    postsApi,
    routerMocks: { dismissTo: (destino) => (destinoFechar = destino) },
  });

  let renderer;
  await act(async () => {
    renderer = create(React.createElement(AdicionarLivroScreen));
  });
  try {
    await act(async () => achar(renderer, 'ListItem').props.onPress());
    await act(async () =>
      achar(
        renderer,
        'PrimaryButton',
        (n) => n.props.label === 'Desejos'
      ).props.onPress()
    );

    await act(async () =>
      achar(
        renderer,
        'PrimaryButton',
        (n) => n.props.label === 'Fechar'
      ).props.onPress()
    );
    assert.equal(destinoFechar.pathname, '/meus-livros');
    assert.ok(destinoFechar.params.livroAdicionado);
  } finally {
    await act(async () => renderer.unmount());
  }
});
