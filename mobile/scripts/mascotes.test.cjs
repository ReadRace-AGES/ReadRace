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

// Usuário no nível 6: os três primeiros desbloqueados, nunca escolheu (padrão O Bruxo).
const mascotes = [
  ['m1', 'O Bruxo', 'o-bruxo', 1, true, true],
  ['m2', 'O Cinzento', 'o-cinzento', 3, true, false],
  ['m3', 'O Sonhador', 'o-sonhador', 5, true, false],
  ['m4', 'A Bruxa Má', 'a-bruxa-ma', 7, false, false],
  ['m5', 'A Bruxa Boa', 'a-bruxa-boa', 9, false, false],
].map(([id, nome, imagem, nivelMinimo, desbloqueado, escolhido]) => ({
  id,
  nome,
  imagem,
  nivelMinimo,
  desbloqueado,
  escolhido,
}));

function montar({ estado, escolher = async () => ({}) }) {
  const chamadas = { voltar: 0, escolhidos: [], erros: [], recarregar: 0 };
  const dependencies = {
    'react-native': {
      View: 'View',
      Text: 'Text',
      Pressable: 'Pressable',
      ScrollView: 'ScrollView',
      ActivityIndicator: 'ActivityIndicator',
      StyleSheet: { create: (v) => v },
    },
    'expo-image': { Image: 'Image' },
    'expo-router': {
      useRouter: () => ({
        canGoBack: () => true,
        back: () => chamadas.voltar++,
      }),
    },
    '@/api/client': { ApiError },
    '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
    '@/components/AppHeader': { AppHeader: 'AppHeader' },
    '@/components/EmptyState': { EmptyState: 'EmptyState' },
    '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '@/components/icons/LockIcon': { LockIcon: 'LockIcon' },
    '@/components/icons/PawIcon': { PawIcon: 'PawIcon' },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showErrorToast: (mensagem) => chamadas.erros.push(mensagem),
      }),
    },
    './api': {
      escolherMascote: (id) => {
        chamadas.escolhidos.push(id);
        return escolher(id);
      },
    },
    './imagens': { arteDoMascote: (imagem) => imagem, SILHUETA: 'silhueta' },
    './useMascotes': {
      useMascotes: () => ({
        estado,
        recarregar: () => chamadas.recarregar++,
      }),
    },
  };
  const { MascotesScreen } = load(
    'features/mascotes/MascotesScreen.tsx',
    dependencies
  );
  let tree;
  act(() => {
    tree = create(React.createElement(MascotesScreen));
  });
  return { tree, chamadas };
}

const cartas = (tree) =>
  tree.root
    .findAllByType('Pressable')
    .filter((p) => p.props.accessibilityState);
const carta = (tree, rotulo) =>
  cartas(tree).find((p) => p.props.accessibilityLabel === rotulo);
const selecionadas = (tree) =>
  cartas(tree)
    .filter((p) => p.props.accessibilityState.selected)
    .map((p) => p.props.accessibilityLabel);
const concluir = (tree) =>
  tree.root
    .findAllByType('PrimaryButton')
    .find((b) => b.props.label === 'Concluir');

test('mostra os 5 na ordem: nível 6 tem 3 abertos e 2 "???" com o nível que desbloqueia', () => {
  const { tree } = montar({ estado: { situacao: 'sucesso', dados: mascotes } });
  const header = tree.root.findByType('AppHeader');
  assert.equal(header.props.title, 'Mascotes');
  assert.equal(header.props.subtitle, 'Personalize seu parceiro de leitura!');
  assert.deepEqual(
    cartas(tree).map((p) => p.props.accessibilityLabel),
    [
      'O Bruxo',
      'O Cinzento',
      'O Sonhador',
      'Mascote bloqueado, desbloqueia no nível 7',
      'Mascote bloqueado, desbloqueia no nível 9',
    ]
  );
  const textos = tree.root.findAllByType('Text').map((t) => t.props.children);
  assert.deepEqual(textos, [
    'O Bruxo',
    'O Cinzento',
    'O Sonhador',
    '???',
    'Desbloqueia no nível 7',
    '???',
    'Desbloqueia no nível 9',
  ]);
  assert.equal(tree.root.findAllByType('LockIcon').length, 2);
  // Carta "???" não seleciona: o toque fica desabilitado.
  assert.equal(cartas(tree).filter((p) => p.props.disabled).length, 2);
  // O padrão de quem nunca escolheu já vem destacado.
  assert.deepEqual(selecionadas(tree), ['O Bruxo']);
  act(() => tree.unmount());
});

test('tocar em O Cinzento e Concluir grava e volta ao perfil', async () => {
  const { tree, chamadas } = montar({
    estado: { situacao: 'sucesso', dados: mascotes },
  });
  act(() => carta(tree, 'O Cinzento').props.onPress());
  assert.deepEqual(selecionadas(tree), ['O Cinzento']);
  await act(async () => concluir(tree).props.onPress());
  assert.deepEqual(chamadas.escolhidos, ['m2']);
  assert.equal(chamadas.voltar, 1);
  act(() => tree.unmount());
});

test('Concluir sem trocar volta sem gravar', async () => {
  const { tree, chamadas } = montar({
    estado: { situacao: 'sucesso', dados: mascotes },
  });
  await act(async () => concluir(tree).props.onPress());
  assert.deepEqual(chamadas.escolhidos, []);
  assert.equal(chamadas.voltar, 1);
  act(() => tree.unmount());
});

test('erro ao concluir avisa e mantém a seleção na tela', async () => {
  const { tree, chamadas } = montar({
    estado: { situacao: 'sucesso', dados: mascotes },
    escolher: async () => {
      throw new ApiError(409, {
        code: 'MASCOTE_BLOQUEADO',
        message: 'Este mascote ainda está bloqueado para o seu nível.',
      });
    },
  });
  act(() => carta(tree, 'O Sonhador').props.onPress());
  await act(async () => concluir(tree).props.onPress());
  assert.equal(chamadas.voltar, 0);
  assert.equal(chamadas.erros.length, 1);
  assert.deepEqual(selecionadas(tree), ['O Sonhador']);
  act(() => tree.unmount());
});

test('carregando no lugar da grade; erro ao abrir permite tentar de novo', () => {
  let { tree, chamadas } = montar({ estado: { situacao: 'carregando' } });
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
  assert.equal(cartas(tree).length, 0);
  act(() => tree.unmount());

  ({ tree, chamadas } = montar({
    estado: { situacao: 'erro', mensagem: 'Falhou.' },
  }));
  const vazio = tree.root.findByType('EmptyState');
  assert.equal(vazio.props.message, 'Falhou.');
  act(() => vazio.props.action.props.onPress());
  assert.equal(chamadas.recarregar, 1);
  act(() => tree.unmount());
});
