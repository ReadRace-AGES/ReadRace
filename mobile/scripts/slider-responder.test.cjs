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

test('slider cede a rolagem e mantém toque, arraste, limites e acessibilidade', () => {
  const values = load('sliderValues.ts');
  const { Slider } = load('Slider.tsx', {
    'react-native': { Text: 'Text', View: 'View' },
    '@/theme': { sizes: { icon: 24 } },
    './sliderValues': values,
  });
  let tree;
  function Tela() {
    const [value, setValue] = React.useState(150);
    return React.createElement(Slider, {
      minimumValue: 10,
      maximumValue: 500,
      value,
      minimumLabel: '10',
      maximumLabel: '500+',
      accessibilityLabel: 'Meta de páginas',
      onValueChange: setValue,
    });
  }
  const control = () =>
    tree.root
      .findAllByType('View')
      .find((node) => node.props.accessibilityRole === 'adjustable');
  act(() => {
    tree = create(React.createElement(Tela));
  });
  act(() =>
    control().props.onLayout({ nativeEvent: { layout: { width: 324 } } })
  );
  assert.equal(control().props.onStartShouldSetResponder(), true);
  assert.equal(control().props.onMoveShouldSetResponder(), true);
  assert.equal(control().props.onResponderTerminationRequest(), true);

  act(() =>
    control().props.onResponderGrant({
      nativeEvent: { pageX: 162, locationX: 162 },
    })
  );
  assert.equal(control().props.accessibilityValue.now, 255);
  act(() => control().props.onResponderMove({ nativeEvent: { pageX: 500 } }));
  assert.equal(control().props.accessibilityValue.now, 500);
  act(() =>
    control().props.onResponderRelease({ nativeEvent: { pageX: -50 } })
  );
  assert.equal(control().props.accessibilityValue.now, 10);

  act(() =>
    control().props.onAccessibilityAction({
      nativeEvent: { actionName: 'increment' },
    })
  );
  assert.equal(control().props.accessibilityValue.now, 11);
  act(() =>
    control().props.onAccessibilityAction({
      nativeEvent: { actionName: 'decrement' },
    })
  );
  assert.equal(control().props.accessibilityValue.now, 10);
  act(() => tree.unmount());
});
