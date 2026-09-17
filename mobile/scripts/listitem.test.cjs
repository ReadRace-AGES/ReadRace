const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const theme = {
  ...require('../src/theme/tokens'),
  textStyles: {},
};
const dependencies = {
  'react-native': {
    View: 'View',
    Text: 'Text',
    Pressable: 'Pressable',
    StyleSheet: { create: (styles) => styles },
  },
  'react-native-svg': {
    __esModule: true,
    default: 'Svg',
    Circle: 'Circle',
    Path: 'Path',
  },
  'expo-image': { Image: 'Image' },
  '@/theme': theme,
};
function load(file) {
  const { outputText } = ts.transpileModule(
    fs.readFileSync(path.join(__dirname, '../src/components', file), 'utf8'),
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
for (const name of ['Card', 'BookCover', 'avatar']) {
  dependencies[`@/components/${name}`] = load(`${name}.tsx`);
}
const { ListItem } = load('ListItem.tsx');
const render = (props) => React.createElement(ListItem, props);

test('seleção depende das props e o toque apenas notifica a tela', () => {
  let calls = 0;
  let tree;
  const props = {
    variant: 'book',
    title: 'Dune',
    selected: false,
    onPress: () => calls++,
  };
  act(() => {
    tree = create(render(props));
  });
  const row = () => tree.root.findByType('Pressable');
  act(() => row().props.onPress());
  assert.equal(calls, 1);
  assert.deepEqual(row().props.accessibilityState, { checked: false });
  act(() => tree.update(render({ ...props, selected: true })));
  assert.deepEqual(row().props.accessibilityState, { checked: true });
  assert.ok(
    tree.root.findAllByType('Text').some((text) => text.props.children === '✓')
  );
  const card = tree.root.findByType(dependencies['@/components/Card'].Card);
  assert.equal(
    card.props.surfaceStyle[1].backgroundColor,
    theme.colors.surfacePink
  );
  act(() => tree.unmount());
});

test('ação e linha são controles irmãos com callbacks independentes', () => {
  let rows = 0,
    actions = 0,
    tree;
  act(() => {
    tree = create(
      render({
        variant: 'community',
        title: 'Clube',
        memberCount: 0,
        onPress: () => rows++,
        action: { label: 'Participar', onPress: () => actions++ },
      })
    );
  });
  const [row, action] = tree.root.findAllByType('Pressable');
  assert.equal(row.parent, action.parent);
  act(() => action.props.onPress());
  assert.equal(actions, 1);
  assert.equal(rows, 0);
  act(() => row.props.onPress());
  assert.equal(rows, 1);
  assert.match(row.props.accessibilityLabel, /0 membros/);
  act(() => tree.unmount());
});

test('opcionais ausentes não criam ação, metadado ou indicador; placeholders são reutilizados', () => {
  let tree;
  act(() => {
    tree = create(render({ variant: 'community', title: 'Café com Letras' }));
  });
  assert.equal(tree.root.findAllByType('Pressable').length, 0);
  assert.deepEqual(
    tree.root.findAllByType('Text').map((node) => node.props.children),
    ['C', 'Café com Letras']
  );
  const title = tree.root
    .findAllByType('Text')
    .find((node) => node.props.children === 'Café com Letras');
  assert.equal(title.props.numberOfLines, 1);
  assert.equal(title.props.ellipsizeMode, 'tail');
  act(() => tree.update(render({ variant: 'book', title: 'Livro' })));
  assert.equal(tree.root.findAllByType('Pressable').length, 0);
  assert.equal(
    tree.root.findAllByType(dependencies['@/components/BookCover'].BookCover)
      .length,
    1
  );
  assert.equal(tree.root.findAllByType('Image').length, 0);
  assert.equal(
    tree.root
      .findAllByType('Text')
      .filter((node) => node.props.children === '✓').length,
    0
  );
  act(() => tree.unmount());
});

test('imagem de comunidade com erro recupera a inicial', () => {
  let tree;
  act(() => {
    tree = create(
      render({
        variant: 'community',
        title: 'Leitores',
        imageUrl: 'https://example.invalid/photo',
      })
    );
  });
  act(() => tree.root.findByType('Image').props.onError());
  assert.equal(tree.root.findAllByType('Image').length, 0);
  assert.ok(
    tree.root.findAllByType('Text').some((node) => node.props.children === 'L')
  );
  act(() => tree.unmount());
});
