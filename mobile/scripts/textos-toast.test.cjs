const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

globalThis.IS_REACT_ACT_ENVIRONMENT = true;
const theme = { ...require('../src/theme/tokens'), textStyles: {} };
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  StyleSheet: { create: (styles) => styles },
};
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
const deps = {
  'react-native': native,
  '@/theme': theme,
  '@/components/Card': { Card: 'Card' },
  './Card': { Card: 'Card' },
  '@/components/avatar': { Avatar: 'Avatar' },
  './avatar': { Avatar: 'Avatar', StreakBadge: 'StreakBadge' },
  './icons/HeartIcon': { HeartIcon: 'HeartIcon' },
  './icons/CommentsIcon': { CommentsIcon: 'CommentsIcon' },
  'react-native-svg': {
    __esModule: true,
    default: 'Svg',
    Circle: 'Circle',
    Path: 'Path',
  },
};
const { ClubeCard } = load('features/feed/ClubeCard.tsx', deps);
const { PostCard } = load('components/PostCard.tsx', deps);

for (const count of [0, 1, 2]) {
  test(`clube: ${count} membro(s) no texto e na acessibilidade`, () => {
    let tree;
    act(() => {
      tree = create(
        React.createElement(ClubeCard, {
          clube: {
            nome: 'Clube',
            livroAtual: { titulo: 'Livro' },
            totalMembros: count,
          },
          onPress() {},
        })
      );
    });
    try {
      const esperado = `${count} ${count === 1 ? 'membro' : 'membros'}`;
      assert.ok(
        tree.root
          .findAllByType('Text')
          .some((n) => n.props.children === esperado)
      );
      assert.ok(
        tree.root
          .findByType('Pressable')
          .props.accessibilityLabel.endsWith(esperado)
      );
    } finally {
      act(() => tree.unmount());
    }
  });
  for (const interativo of [false, true]) {
    test(`post: ${count} curtida(s)/comentário(s), interativo=${interativo}`, () => {
      let tree;
      act(() => {
        tree = create(
          React.createElement(PostCard, {
            authorName: 'Leitor',
            timeAgo: 'Agora',
            text: '',
            likes: count,
            comments: count,
            likedByMe: true,
            ...(interativo ? { onLikePress() {}, onCommentsPress() {} } : {}),
          })
        );
      });
      try {
        const labels = tree.root
          .findAll((n) => typeof n.props.accessibilityLabel === 'string')
          .map((n) => n.props.accessibilityLabel);
        assert.ok(
          labels.includes(
            `${count} ${count === 1 ? 'curtida' : 'curtidas'}, curtido por você`
          )
        );
        assert.ok(
          labels.includes(
            `${count} ${count === 1 ? 'comentário' : 'comentários'}`
          )
        );
        if (interativo) {
          assert.deepEqual(
            tree.root.findAllByType('Pressable')[0].props.accessibilityState,
            { disabled: false, selected: true }
          );
        }
      } finally {
        act(() => tree.unmount());
      }
    });
  }
}

for (const inset of [0, 34]) {
  test(`toast acompanha medida, foco e desmontagem das abas; inset=${inset}`, () => {
    let focused = true;
    let pathname = '/feed';
    let controls;
    const router = {
      usePathname: () => pathname,
      useFocusEffect: (effect) =>
        React.useEffect(
          () => (focused ? effect() : undefined),
          [effect, focused]
        ),
    };
    const animated = {
      Value: class {
        constructor(value) {
          this.value = value;
        }
      },
      View: 'AnimatedView',
      timing: () => ({ start: (callback) => callback?.({ finished: true }) }),
    };
    const { Toast, useToast } = load('components/toast.tsx', {
      'react-native': { ...native, Animated: animated },
      'react-native-safe-area-context': {
        useSafeAreaInsets: () => ({ bottom: inset }),
      },
      '@/theme': theme,
    });
    const provider = load('components/toast-provider.tsx', {
      'expo-router': router,
      './toast': { Toast, useToast },
    });
    const iconDeps = {};
    for (const name of [
      'BookTabIcon',
      'HomeTabIcon',
      'PersonTabIcon',
      'SearchTabIcon',
      'StarTabIcon',
    ])
      iconDeps[name] = name;
    const { BottomTabBar } = load('components/BottomTabBar.tsx', {
      'react-native': native,
      '@/theme': theme,
      '@/components/TabIcons': iconDeps,
    });
    function Tabs(props) {
      return props.tabBar({
        state: { index: 0, routes: [{ name: '(feed)', key: 'feed' }] },
        navigation: { emit() {}, navigate() {} },
        insets: { bottom: inset },
      });
    }
    Tabs.Screen = () => null;
    const { default: TabsLayout } = load('app/(tabs)/_layout.tsx', {
      'expo-router': { ...router, Tabs },
      '@/components/BottomTabBar': { BottomTabBar },
      '@/components/toast-provider': provider,
    });
    function Probe() {
      controls = provider.useToastContext();
      return null;
    }
    function App({ mounted = true }) {
      return React.createElement(
        provider.ToastProvider,
        null,
        React.createElement(Probe),
        mounted && React.createElement(TabsLayout)
      );
    }
    let tree;
    act(() => {
      tree = create(React.createElement(App));
    });
    const bottom = () => tree.root.findByType(Toast).props.tabBarHeight;
    const offset = () =>
      tree.root
        .findAllByType('View')
        .find((n) => n.props.pointerEvents === 'none').props.style[1].bottom;
    const measure = (height) =>
      act(() => {
        tree.root
          .findByType(BottomTabBar)
          .findAllByType('View')[0]
          .props.onLayout({ nativeEvent: { layout: { height } } });
      });
    try {
      act(() => controls.showToast());
      assert.equal(offset(), inset + theme.spacing[6]);
      const height =
        theme.spacing[3] + theme.sizes.navHeight + (inset || theme.spacing[6]);
      measure(height);
      assert.equal(bottom(), height);
      assert.equal(offset(), height + theme.spacing[6]); // inset já está na medida
      measure(height + theme.spacing[2]);
      assert.equal(offset(), height + theme.spacing[2] + theme.spacing[6]);
      // Trocar a aba ou abrir uma tela interna mantém o navegador de abas em foco.
      pathname = '/outra-tela-interna';
      act(() => tree.update(React.createElement(App)));
      assert.equal(bottom(), height + theme.spacing[2]);
      // A pilha raiz pode cobrir as abas sem desmontá-las.
      focused = false;
      act(() => tree.update(React.createElement(App)));
      act(() => controls.showToast());
      assert.equal(bottom(), 0);
      assert.equal(offset(), inset + theme.spacing[6]);
      measure(height);
      assert.equal(bottom(), 0); // layout de uma barra fora de foco não afasta o toast
      focused = true;
      act(() => tree.update(React.createElement(App)));
      assert.equal(bottom(), height);
      act(() => tree.update(React.createElement(App, { mounted: false })));
      assert.equal(bottom(), 0);
      assert.equal(offset(), inset + theme.spacing[6]);
    } finally {
      act(() => tree.unmount());
    }
  });
}
