import assert from 'node:assert/strict';
import test from 'node:test';

import {
  clampSliderValue,
  sliderValueAtPosition,
} from '../src/components/sliderValues.ts';

test('prende valores externos aos limites e arredonda páginas', () => {
  assert.equal(clampSliderValue(700, 10, 500), 500);
  assert.equal(clampSliderValue(-20, 10, 500), 10);
  assert.equal(clampSliderValue(150.6, 10, 500), 151);
});

test('arrastar além das duas pontas nunca ultrapassa os limites', () => {
  for (let position = -100; position <= 400; position += 0.5) {
    const value = sliderValueAtPosition(position, 300, 10, 500);
    assert.ok(Number.isInteger(value));
    assert.ok(value >= 10 && value <= 500);
  }
  assert.equal(sliderValueAtPosition(-100, 300, 10, 500), 10);
  assert.equal(sliderValueAtPosition(400, 300, 10, 500), 500);
});

test('posição proporcional funciona em larguras e faixas diferentes', () => {
  assert.equal(sliderValueAtPosition(100, 350, 10, 500), 150);
  assert.equal(sliderValueAtPosition(200, 700, 10, 500), 150);
  assert.equal(sliderValueAtPosition(75, 300, -10, 10), -5);
  assert.equal(sliderValueAtPosition(150, 300, -10, 10), 0);
});
