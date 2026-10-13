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

test('busca não renderiza sugestões antigas ao mudar termo, tipo ou tentar novamente', async () => {
  const requests = [];
  const { ApiError } = load('api/client.ts', {});
  const { useBusca } = load('features/busca/useBusca.ts', {
    '@/api/client': { ApiError },
    './api': {
      buscar: (termo, tipo, signal) =>
        new Promise((resolve) =>
          requests.push({ termo, tipo, signal, resolve })
        ),
    },
  });
  const renderizacoes = [];
  let recarregar;
  function Busca({ termo, tipo }) {
    const estado = useBusca(termo, tipo);
    recarregar = estado.recarregar;
    renderizacoes.push(estado.busca);
    return React.createElement('View');
  }
  let tree;
  const render = (termo, tipo = 'livros') =>
    React.createElement(Busca, { termo, tipo });
  const waitForSearch = () =>
    act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  const receive = (index) =>
    act(async () =>
      requests[index].resolve({
        tipo: requests[index].tipo,
        itens: [{ id: `resultado-${index}` }],
      })
    );
  await act(async () => {
    tree = create(render('Livro'));
  });
  await waitForSearch();
  await receive(0);
  renderizacoes.length = 0;
  act(() => tree.update(render('Outro')));
  assert.ok(renderizacoes.every((busca) => busca.situacao === 'carregando'));
  await waitForSearch();
  await receive(1);
  renderizacoes.length = 0;
  act(() => tree.update(render('Outro', 'usuarios')));
  assert.ok(renderizacoes.every((busca) => busca.situacao === 'carregando'));
  await waitForSearch();
  await receive(2);
  renderizacoes.length = 0;
  act(() => recarregar());
  assert.ok(renderizacoes.every((busca) => busca.situacao === 'carregando'));
  await waitForSearch();
  await receive(3);
  renderizacoes.length = 0;
  act(() => tree.update(render('   ', 'usuarios')));
  assert.ok(renderizacoes.every((busca) => busca.situacao === 'inicial'));
  act(() => tree.unmount());
});

test('Feed recarrega uma vez por criação e exibe os dados consultados sem alterar a lista ao fechar', async () => {
  const requests = [];
  const { ApiError } = load('api/client.ts', {});
  const api = load('features/feed/api.ts', {
    '@/api/client': {
      apiGet: (url, { signal }) =>
        new Promise((resolve, reject) =>
          requests.push({ url, signal, resolve, reject })
        ),
    },
  });
  const hooks = load('features/feed/useFeedComunidades.ts', {
    '@/api/client': { ApiError },
    './api': api,
  });
  const components = {
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      Pressable: 'Pressable',
      ScrollView: 'ScrollView',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      View: 'View',
    },
    'react-native-svg': {
      __esModule: true,
      default: 'Svg',
      Circle: 'Circle',
      Path: 'Path',
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/Card': { Card: 'Card' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/SegmentedTabs': { SegmentedTabs: 'SegmentedTabs' },
  };
  const { ClubeCard } = load('features/feed/ClubeCard.tsx', components);
  const { FeedComunidadesView } = load(
    'features/feed/FeedComunidadesView.tsx',
    {
      ...components,
      './ClubeCard': { ClubeCard },
    }
  );
  const { default: FeedScreen } = load('app/(tabs)/(feed)/feed.tsx', {
    'expo-router': {
      router: { push: () => assert.fail('Criar não deve navegar') },
      useFocusEffect: (callback) => React.useEffect(callback, [callback]),
    },
    'react-native': { Keyboard: { dismiss: () => {} } },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => {} }),
    },
    '@/features/feed/FeedComunidadesView': {
      FeedComunidadesView,
    },
    '@/features/feed/CriarGrupoSheet': { CriarGrupoSheet: 'CriarGrupoSheet' },
    '@/features/feed/useFeedComunidades': hooks,
  });
  let tree;
  const view = () => tree.root.findByType(FeedComunidadesView);
  const sheet = () => tree.root.findByType('CriarGrupoSheet');
  const vazio = {
    usuario: { nome: 'Leitor', sequenciaDias: 3 },
    clubes: [],
    comunidades: [],
  };
  const clube = {
    id: '10000000-0000-0000-0000-000000000001',
    nome: 'Leitores',
    capaUrl: 'https://example.com/capa.jpg',
    livroAtual: { titulo: 'Livro', autor: 'Autor' },
    totalMembros: 2,
  };
  const atualizado = {
    ...vazio,
    clubes: [clube],
    comunidades: [
      { id: 'comunidade', nome: 'Comunidade', capaUrl: null, totalMembros: 5 },
    ],
  };
  await act(async () => {
    tree = create(React.createElement(FeedScreen));
  });
  assert.equal(view().props.feed.situacao, 'carregando');
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/api/feed/comunidades');
  await act(async () => requests[0].resolve(vazio));
  const instancia = view();
  const abas = tree.root.findByType('SegmentedTabs');
  assert.deepEqual(view().props.feed.dados.clubes, []);

  act(() => view().props.onCriarGrupoPress());
  assert.equal(sheet().props.visible, true);
  act(() => sheet().props.onClose());
  assert.equal(sheet().props.visible, false);
  assert.equal(requests.length, 1);
  assert.deepEqual(view().props.feed.dados, vazio);

  act(() => view().props.onCriarGrupoPress());
  act(() => sheet().props.onSuccess());
  assert.equal(requests.length, 2);
  assert.equal(requests[1].url, '/api/feed/comunidades');
  assert.equal(sheet().props.visible, true);
  assert.deepEqual(view().props.feed.dados, vazio);
  await act(async () => requests[1].resolve(atualizado));
  assert.equal(view(), instancia);
  assert.deepEqual(view().props.feed.dados, atualizado);
  assert.deepEqual(view().props.feed.dados.clubes[0], clube);
  assert.equal(tree.root.findByType('SegmentedTabs'), abas);
  assert.equal(tree.root.findByType('Avatar').props.photoUrl, clube.capaUrl);
  assert.ok(
    tree.root
      .findAllByType('Pressable')
      .some(
        (node) =>
          node.props.accessibilityLabel === 'Leitores, Livro - Autor, 2 membros'
      )
  );
  act(() => sheet().props.onClose());
  assert.equal(sheet().props.visible, false);
  assert.equal(requests.length, 2);

  act(() => view().props.onCriarGrupoPress());
  act(() => {
    sheet().props.onSuccess();
    sheet().props.onClose();
  });
  assert.equal(requests.length, 3);
  await act(async () =>
    requests[2].reject(new ApiError(503, { message: 'Feed indisponível.' }))
  );
  assert.deepEqual(view().props.feed.dados, atualizado);
  act(() => view().props.onCriarGrupoPress());
  act(() => sheet().props.onClose());
  assert.equal(requests.length, 3);
  assert.equal(view(), instancia);
  act(() => tree.unmount());
});

test('envio valida o DTO, preserva erros, confirma sucesso e permite novo clube sem repetir envio ou recarga', async () => {
  const posts = [];
  let fechamentos = 0;
  let atualizacoes = 0;
  const { ApiError } = load('api/client.ts', {});
  const api = load('features/feed/api.ts', {
    '@/api/client': {
      apiRequest: (url, init) =>
        new Promise((resolve, reject) =>
          posts.push({ url, init, resolve, reject })
        ),
    },
  });
  const livro = {
    id: '10000000-0000-0000-0000-000000000001',
    titulo: 'Livro',
    autor: 'Autor',
    capa: null,
    totalPaginas: 100,
  };
  const criador = {
    id: '20000000-0000-0000-0000-000000000001',
    nome: 'Criador',
    username: 'criador',
    avatar: null,
    titulo: 'Leitor',
  };
  const ana = {
    ...criador,
    id: '20000000-0000-0000-0000-000000000002',
    nome: 'Ana',
    username: 'ana',
  };
  const { CriarGrupoSheet } = load('features/feed/CriarGrupoSheet.tsx', {
    '@/api/client': { ApiError },
    './api': api,
    '@/components/BookCover': { BookCover: 'BookCover' },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      Pressable: 'Pressable',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      TextInput: 'TextInput',
      View: 'View',
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/BottomSheet': { BottomSheet: 'BottomSheet' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/features/busca/useBusca': {
      useBusca: (termo, tipo) => ({
        busca: termo
          ? {
              situacao: 'sucesso',
              dados: {
                tipo,
                itens: tipo === 'livros' ? [livro] : [criador, ana],
              },
            }
          : { situacao: 'inicial' },
        recarregar: () => {},
      }),
    },
    '@/features/perfil/usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'sucesso', dados: criador },
        recarregar: () => {},
      }),
    },
  });
  let tree;
  const render = (visible) =>
    React.createElement(CriarGrupoSheet, {
      visible,
      onSuccess: () => atualizacoes++,
      onClose: () => {
        fechamentos++;
        tree.update(render(false));
      },
    });
  const field = (label) =>
    tree.root
      .findAllByType('TextInput')
      .find((node) => node.props.accessibilityLabel === label);
  const change = (label, value) =>
    act(() => field(label).props.onChangeText(value));
  const button = () => tree.root.findByType('PrimaryButton');
  const errors = () =>
    tree.root
      .findAllByType('Text')
      .filter((node) => node.props.accessibilityRole === 'alert');
  const selectBook = () => {
    change('Título do Livro', 'Livro');
    act(() => tree.root.findByType('ListItem').props.onPress());
  };
  const prepare = () => {
    change('Nome do Grupo', 'Leitores');
    selectBook();
  };
  await act(async () => {
    tree = create(render(true));
  });
  assert.equal(button().props.disabled, true);
  act(() => {
    button().props.onPress();
  });
  change('Nome do Grupo', 'Leitores');
  assert.equal(button().props.disabled, true);
  change('Título do Livro', 'Livro');
  assert.equal(button().props.disabled, true);
  act(() => tree.root.findByType('ListItem').props.onPress());
  for (const invalido of ['', '   ', 'x'.repeat(121)]) {
    change('Nome do Grupo', invalido);
    assert.equal(button().props.disabled, true);
    act(() => {
      button().props.onPress();
    });
  }
  assert.equal(posts.length, 0);
  change('Nome do Grupo', 'x'.repeat(120));
  assert.equal(button().props.disabled, false);
  change('Título do Livro', 'Título editado');
  assert.equal(button().props.disabled, true);
  selectBook();
  change('Nome do Grupo', '  Leitores  ');
  change('Descrição, opcional', '  Descrição  ');
  change('Adicionar membros', 'Ana');
  act(() =>
    tree.root
      .findAllByType('Pressable')
      .find((node) =>
        node.props.accessibilityLabel?.startsWith('Adicionar Ana')
      )
      .props.onPress()
  );
  const submit = button().props.onPress;
  act(() => {
    submit();
    submit();
  });
  assert.equal(posts.length, 1);
  assert.equal(posts[0].url, '/api/clubes');
  assert.equal(posts[0].init.method, 'POST');
  assert.deepEqual(JSON.parse(posts[0].init.body), {
    nome: 'Leitores',
    descricao: 'Descrição',
    livroId: livro.id,
    membros: [ana.id],
  });
  assert.equal(button().props.loading, true);
  assert.equal(button().props.disabled, true);
  assert.equal(tree.root.findByType('BottomSheet').props.dismissible, false);
  for (const input of tree.root.findAllByType('TextInput'))
    assert.equal(input.props.editable, false);
  assert.equal(
    tree.root
      .findAllByType('Pressable')
      .find((node) => node.props.accessibilityLabel?.startsWith('Remover Ana'))
      .props.disabled,
    true
  );
  await act(async () =>
    posts[0].reject(new ApiError(400, { message: 'Clube inválido.' }))
  );
  assert.equal(errors()[0].props.children, 'Clube inválido.');
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  assert.equal(button().props.label, 'Criar Grupo');
  assert.equal(field('Nome do Grupo').props.value, '  Leitores  ');
  assert.equal(field('Descrição, opcional').props.value, '  Descrição  ');
  assert.equal(field('Título do Livro').props.value, 'Livro');
  assert.equal(field('Autor, somente leitura').props.value, 'Autor');
  assert.equal(button().props.loading, false);
  assert.equal(button().props.disabled, false);
  assert.equal(fechamentos, 0);
  assert.equal(atualizacoes, 0);
  act(() => {
    button().props.onPress();
  });
  assert.equal(errors().length, 0);
  await act(async () => posts[1].reject(new Error('offline')));
  assert.equal(
    errors()[0].props.children,
    'Não foi possível criar o grupo. Tente novamente.'
  );
  assert.deepEqual(
    JSON.parse(posts[1].init.body),
    JSON.parse(posts[0].init.body)
  );
  act(() => {
    button().props.onPress();
  });
  await act(async () =>
    posts[2].resolve({
      id: 'clube',
      nome: 'Leitores confirmados',
      livro: {
        titulo: 'Livro confirmado',
        capaUrl: 'https://example.com/livro.jpg',
      },
    })
  );
  assert.equal(fechamentos, 0);
  assert.equal(atualizacoes, 1);
  assert.equal(tree.root.findByType('BottomSheet').props.visible, true);
  assert.equal(tree.root.findByType('BottomSheet').props.dismissible, true);
  assert.equal(tree.root.findAllByType('TextInput').length, 0);
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some((node) => node.props.children === 'Leitores confirmados')
  );
  assert.ok(
    tree.root.findAllByType('Text').some((node) => node.props.children === '✓')
  );
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some((node) => node.props.children === 'Clube criado com sucesso!')
  );
  assert.equal(
    tree.root.findByType('BookCover').props.source,
    'https://example.com/livro.jpg'
  );
  assert.equal(
    tree.root.findByType('BookCover').props.accessibilityLabel,
    'Capa de Livro confirmado'
  );
  assert.equal(button().props.label, 'Criar novo clube');
  act(() => button().props.onPress());
  assert.equal(posts.length, 3);
  assert.equal(atualizacoes, 1);
  assert.equal(fechamentos, 0);
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  assert.equal(button().props.label, 'Criar Grupo');
  assert.equal(button().props.disabled, true);
  for (const input of tree.root.findAllByType('TextInput'))
    assert.equal(input.props.value, '');
  assert.equal(
    tree.root
      .findAllByType('Pressable')
      .filter((node) => node.props.accessibilityLabel?.startsWith('Remover '))
      .length,
    0
  );
  prepare();
  act(() => {
    button().props.onPress();
  });
  assert.equal(posts.length, 4);
  assert.deepEqual(JSON.parse(posts[3].init.body), {
    nome: 'Leitores',
    descricao: null,
    livroId: livro.id,
    membros: [],
  });
  await act(async () =>
    posts[3].resolve({
      id: 'clube-2',
      nome: 'Leitores',
      livro: { titulo: 'Livro', capaUrl: null },
    })
  );
  assert.equal(atualizacoes, 2);
  assert.equal(fechamentos, 0);
  assert.equal(button().props.label, 'Criar novo clube');
  assert.equal(tree.root.findByType('BookCover').props.source, null);
  act(() =>
    tree.root
      .findAllByType('Pressable')
      .find((node) => node.props.accessibilityLabel === 'Fechar')
      .props.onPress()
  );
  assert.equal(fechamentos, 1);
  assert.equal(atualizacoes, 2);
  assert.equal(posts.length, 4);
  assert.equal(tree.root.findByType('BottomSheet').props.visible, false);

  act(() => tree.update(render(true)));
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  for (const input of tree.root.findAllByType('TextInput'))
    assert.equal(input.props.value, '');
  prepare();
  act(() => {
    button().props.onPress();
  });
  act(() => tree.update(render(false)));
  assert.equal(posts[4].init.signal.aborted, true);
  act(() => tree.update(render(true)));
  prepare();
  act(() => {
    button().props.onPress();
  });
  await act(async () =>
    posts[4].resolve({
      id: 'antigo',
      nome: 'Antigo',
      livro: { titulo: 'Antigo', capaUrl: null },
    })
  );
  assert.equal(fechamentos, 1);
  assert.equal(atualizacoes, 2);
  assert.equal(button().props.loading, true);
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  assert.equal(field('Nome do Grupo').props.value, 'Leitores');
  act(() => tree.update(render(false)));
  act(() => tree.update(render(true)));
  await act(async () =>
    posts[5].reject(new ApiError(500, { message: 'Erro antigo' }))
  );
  assert.equal(errors().length, 0);
  assert.equal(button().props.loading, false);
  assert.equal(field('Nome do Grupo').props.value, '');
  assert.equal(fechamentos, 1);
  assert.equal(atualizacoes, 2);
  prepare();
  act(() => {
    button().props.onPress();
  });
  act(() => tree.unmount());
  assert.equal(posts[6].init.signal.aborted, true);
  await act(async () =>
    posts[6].resolve({
      id: 'desmontado',
      nome: 'Desmontado',
      livro: { titulo: 'Livro', capaUrl: null },
    })
  );
  assert.equal(fechamentos, 1);
  assert.equal(atualizacoes, 2);
});

test('busca de livro seleciona, invalida e descarta respostas após troca, seleção e fechamento', async () => {
  const requests = [];
  const { ApiError } = load('api/client.ts', {});
  const api = load('features/busca/api.ts', {
    '@/api/client': {
      apiGet: (url, { signal }) =>
        new Promise(
          (resolve, reject) =>
            url.includes('tipo=livros') &&
            requests.push({ url, signal, resolve, reject })
        ),
    },
  });
  const hooks = load('features/busca/useBusca.ts', {
    '@/api/client': { ApiError },
    './api': api,
  });
  const { CriarGrupoSheet } = load('features/feed/CriarGrupoSheet.tsx', {
    '@/api/client': { ApiError },
    './api': {
      criarClube: () => assert.fail('Este teste não envia o formulário'),
    },
    '@/components/BookCover': { BookCover: 'BookCover' },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      Pressable: 'Pressable',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      TextInput: 'TextInput',
      View: 'View',
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/BottomSheet': { BottomSheet: 'BottomSheet' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/features/busca/useBusca': hooks,
    '@/features/perfil/usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'sucesso', dados: { id: 'criador' } },
        recarregar: () => {},
      }),
    },
  });
  let tree;
  const render = (visible) =>
    React.createElement(CriarGrupoSheet, {
      visible,
      onClose: () => tree.update(render(false)),
    });
  const field = (label) =>
    tree.root
      .findAllByType('TextInput')
      .find((node) => node.props.accessibilityLabel === label);
  const change = (label, value) =>
    act(() => field(label).props.onChangeText(value));
  const search = (value) => change('Título do Livro', value);
  const suggestions = () => tree.root.findAllByType('ListItem');
  const empty = () => tree.root.findAllByType('EmptyState');
  const loading = () =>
    tree.root
      .findAllByType('ActivityIndicator')
      .filter((node) => node.props.accessibilityLabel === 'Carregando');
  const waitForSearch = () =>
    act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  const receive = (index, itens) =>
    act(async () => requests[index].resolve({ tipo: 'livros', itens }));
  const livro = {
    id: 'livro-148',
    titulo: 'C# & Livros',
    autor: 'Ana',
    capa: null,
    totalPaginas: 100,
  };

  await act(async () => {
    tree = create(render(true));
  });
  search('   ');
  await waitForSearch();
  assert.equal(requests.length, 0);
  assert.equal(empty().length, 0);

  change('Nome do Grupo', 'Leitores');
  change('Descrição, opcional', 'Descrição preservada');
  change('Adicionar membros', 'Texto preservado');
  search('C');
  search('C# &');
  assert.equal(loading().length, 1);
  await waitForSearch();
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/api/busca?q=C%23%20%26&tipo=livros');

  search('C# & Livros');
  assert.equal(requests[0].signal.aborted, true);
  await receive(0, []);
  assert.equal(empty().length, 0);
  assert.equal(loading().length, 1);
  await waitForSearch();
  await receive(1, [livro]);
  assert.equal(loading().length, 0);
  assert.equal(suggestions()[0].props.title, livro.titulo);
  assert.equal(suggestions()[0].props.subtitle, livro.autor);
  act(() => suggestions()[0].props.onPress());
  assert.equal(requests[1].signal.aborted, true);
  assert.equal(field('Título do Livro').props.value, livro.titulo);
  assert.equal(field('Autor, somente leitura').props.value, livro.autor);
  assert.equal(field('Autor, somente leitura').props.editable, false);
  assert.equal(suggestions().length, 0);
  await waitForSearch();
  assert.equal(requests.length, 2);
  assert.equal(tree.root.findByType('PrimaryButton').props.disabled, false);

  search('Outro');
  assert.equal(field('Autor, somente leitura').props.value, '');
  await waitForSearch();
  await receive(2, []);
  assert.equal(
    empty()[0].props.message,
    'Livro não encontrado. Adicione o livro em Meus Livros antes de criar o clube.'
  );
  search('');
  assert.equal(empty().length, 0);

  search('Falha');
  await waitForSearch();
  await act(async () =>
    requests[3].reject(new ApiError(503, { message: 'Catálogo indisponível.' }))
  );
  assert.equal(empty()[0].props.message, 'Catálogo indisponível.');
  assert.equal(field('Título do Livro').props.value, 'Falha');
  assert.equal(field('Nome do Grupo').props.value, 'Leitores');
  assert.equal(
    field('Descrição, opcional').props.value,
    'Descrição preservada'
  );
  assert.equal(field('Adicionar membros').props.value, 'Texto preservado');
  act(() => empty()[0].props.action.props.onPress());
  await waitForSearch();
  await receive(4, [{ ...livro, autor: null }]);
  act(() => suggestions()[0].props.onPress());
  assert.equal(field('Autor, somente leitura').props.value, '');

  act(() => tree.root.findByType('BottomSheet').props.onClose());
  act(() => tree.update(render(true)));
  for (const input of tree.root.findAllByType('TextInput'))
    assert.equal(input.props.value, '');
  assert.equal(empty().length, 0);
  assert.equal(loading().length, 0);
  search('Fechar');
  await waitForSearch();
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  assert.equal(requests[5].signal.aborted, true);
  act(() => tree.update(render(true)));
  await receive(5, [livro]);
  assert.equal(suggestions().length, 0);
  assert.equal(field('Título do Livro').props.value, '');
  assert.equal(field('Autor, somente leitura').props.value, '');

  search('Falha antiga');
  await waitForSearch();
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  act(() => tree.update(render(true)));
  await act(async () => requests[6].reject(new Error('offline')));
  assert.equal(empty().length, 0);
  search('Cancelar debounce');
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  await waitForSearch();
  assert.equal(requests.length, 7);
  act(() => tree.unmount());
});

test('membros excluem criador e duplicatas, preservam formulário e cancelam buscas antigas', async () => {
  const requests = [];
  const perfis = [];
  const { ApiError } = load('api/client.ts', {});
  const client = {
    ApiError,
    apiGet: (url, { signal }) =>
      new Promise((resolve, reject) => {
        const chamada = { url, signal, resolve, reject };
        if (url === '/api/me/perfil') perfis.push(chamada);
        else requests.push(chamada);
      }),
  };
  const hooks = load('features/busca/useBusca.ts', {
    '@/api/client': client,
    './api': load('features/busca/api.ts', { '@/api/client': client }),
  });
  const perfilHooks = load('features/perfil/usePerfil.ts', {
    '@/api/client': client,
    './api': load('features/perfil/api.ts', { '@/api/client': client }),
  });
  const { CriarGrupoSheet } = load('features/feed/CriarGrupoSheet.tsx', {
    '@/api/client': { ApiError },
    './api': {
      criarClube: () => assert.fail('Este teste não envia o formulário'),
    },
    '@/components/BookCover': { BookCover: 'BookCover' },
    'react-native': {
      ActivityIndicator: 'ActivityIndicator',
      Pressable: 'Pressable',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      TextInput: 'TextInput',
      View: 'View',
    },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/BottomSheet': { BottomSheet: 'BottomSheet' },
    '@/components/avatar': { Avatar: 'Avatar' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/icons/SearchIcon': { SearchIcon: 'SearchIcon' },
    '@/components/ListItem': { ListItem: 'ListItem' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/features/busca/useBusca': hooks,
    '@/features/perfil/usePerfil': perfilHooks,
  });
  let tree;
  const render = (visible) =>
    React.createElement(CriarGrupoSheet, {
      visible,
      onClose: () => tree.update(render(false)),
    });
  const field = (label) =>
    tree.root
      .findAllByType('TextInput')
      .find((node) => node.props.accessibilityLabel === label);
  const change = (label, value) =>
    act(() => field(label).props.onChangeText(value));
  const search = (value) => change('Adicionar membros', value);
  const actions = (prefix) =>
    tree.root
      .findAllByType('Pressable')
      .filter((node) => node.props.accessibilityLabel?.startsWith(prefix));
  const waitForSearch = () =>
    act(async () => new Promise((resolve) => setTimeout(resolve, 320)));
  const receive = (index, itens, tipo = 'usuarios') =>
    act(async () => requests[index].resolve({ tipo, itens }));
  const criador = {
    id: 'criador',
    nome: 'Criador',
    username: 'criador',
    avatar: null,
    titulo: 'Leitor',
  };
  const ana = { ...criador, id: 'ana', nome: 'Ana', username: 'ana' };
  const bia = { ...criador, id: 'bia', nome: 'Bia', username: 'bia' };

  await act(async () => {
    tree = create(render(false));
  });
  assert.equal(perfis.length, 0);
  act(() => tree.update(render(true)));
  assert.equal(perfis.length, 1);
  search('   ');
  await waitForSearch();
  assert.equal(requests.length, 0);
  assert.equal(tree.root.findAllByType('EmptyState').length, 0);

  change('Nome do Grupo', 'Leitores');
  change('Descrição, opcional', 'Descrição');
  change('Título do Livro', 'Livro');
  await waitForSearch();
  await receive(
    0,
    [{ id: 'livro', titulo: 'Livro', autor: 'Autor', capa: null }],
    'livros'
  );
  act(() => tree.root.findByType('ListItem').props.onPress());
  search('A');
  search('Ana & Bia');
  await waitForSearch();
  assert.equal(requests.length, 2);
  assert.equal(requests[1].url, '/api/busca?q=Ana%20%26%20Bia&tipo=usuarios');
  await receive(1, [criador, ana, bia]);
  assert.equal(actions('Adicionar ').length, 0);
  assert.equal(
    tree.root.findByType('ActivityIndicator').props.accessibilityLabel,
    'Carregando membros'
  );
  await act(async () =>
    perfis[0].reject(new ApiError(503, { message: 'Perfil indisponível.' }))
  );
  assert.equal(
    tree.root.findByType('EmptyState').props.message,
    'Perfil indisponível.'
  );
  act(() => tree.root.findByType('EmptyState').props.action.props.onPress());
  await act(async () => perfis[1].resolve(criador));
  assert.equal(actions('Adicionar ').length, 2);
  const adicionarAna = actions('Adicionar Ana')[0].props.onPress;
  act(() => adicionarAna());
  act(() => adicionarAna());
  assert.equal(actions('Remover Ana').length, 1);
  assert.equal(actions('Adicionar ').length, 0);
  assert.equal(field('Adicionar membros').props.value, '');
  assert.equal(requests[1].signal.aborted, true);

  search('Bia');
  await waitForSearch();
  await receive(2, [criador, ana, bia]);
  assert.equal(actions('Adicionar ').length, 1);
  act(() => actions('Adicionar Bia')[0].props.onPress());
  act(() => actions('Remover Ana')[0].props.onPress());
  assert.equal(actions('Remover ').length, 1);
  assert.equal(field('Nome do Grupo').props.value, 'Leitores');
  assert.equal(field('Descrição, opcional').props.value, 'Descrição');
  assert.equal(field('Título do Livro').props.value, 'Livro');
  assert.equal(field('Autor, somente leitura').props.value, 'Autor');
  assert.equal(tree.root.findByType('PrimaryButton').props.disabled, false);

  search('Falha');
  await waitForSearch();
  await act(async () =>
    requests[3].reject(new ApiError(503, { message: 'Busca indisponível.' }))
  );
  assert.equal(
    tree.root.findByType('EmptyState').props.message,
    'Busca indisponível.'
  );
  assert.equal(actions('Remover Bia').length, 1);
  assert.equal(field('Adicionar membros').props.value, 'Falha');
  act(() => tree.root.findByType('EmptyState').props.action.props.onPress());
  await waitForSearch();
  await receive(4, [criador, bia]);
  assert.equal(
    tree.root.findByType('EmptyState').props.message,
    'Nenhum usuário encontrado.'
  );

  search('Antiga');
  await waitForSearch();
  search('Nova');
  assert.equal(requests[5].signal.aborted, true);
  await receive(5, [ana]);
  assert.equal(actions('Adicionar ').length, 0);
  await waitForSearch();
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  assert.equal(requests[6].signal.aborted, true);
  assert.equal(perfis[1].signal.aborted, true);
  act(() => tree.update(render(true)));
  await receive(6, [ana]);
  await act(async () => perfis[2].resolve(criador));
  assert.equal(actions('Adicionar ').length, 0);
  assert.equal(actions('Remover ').length, 0);
  for (const input of tree.root.findAllByType('TextInput'))
    assert.equal(input.props.value, '');

  search('Erro antigo');
  await waitForSearch();
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  act(() => tree.update(render(true)));
  await act(async () => requests[7].reject(new Error('offline')));
  assert.equal(tree.root.findAllByType('EmptyState').length, 0);
  search('Cancelar debounce');
  act(() => tree.root.findByType('BottomSheet').props.onClose());
  await waitForSearch();
  assert.equal(requests.length, 8);
  assert.equal(perfis[3].signal.aborted, true);
  await act(async () => perfis[3].resolve(criador));
  act(() => tree.unmount());
});
