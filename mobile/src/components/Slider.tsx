import { useEffect, useRef, useState } from 'react';
import { Text, View, type GestureResponderEvent } from 'react-native';

import { sizes } from '@/theme';

import { clampSliderValue, sliderValueAtPosition } from './sliderValues';

export type SliderProps = {
  minimumValue: number;
  maximumValue: number;
  value: number;
  minimumLabel: string;
  maximumLabel: string;
  accessibilityLabel: string;
  onValueChange: (value: number) => void;
};

/** Controle de inteiros. O valor selecionado e sua unidade pertencem à tela. */
export function Slider({
  minimumValue,
  maximumValue,
  value,
  minimumLabel,
  maximumLabel,
  accessibilityLabel,
  onValueChange,
}: SliderProps) {
  const [width, setWidth] = useState(0);
  const origin = useRef(0);
  const lastNotified = useRef<number | null>(null);

  if (
    !Number.isSafeInteger(minimumValue) ||
    !Number.isSafeInteger(maximumValue) ||
    maximumValue <= minimumValue ||
    !Number.isFinite(value)
  ) {
    throw new Error(
      'Slider exige limites inteiros, máximo maior que mínimo e valor finito.'
    );
  }

  const selected = clampSliderValue(value, minimumValue, maximumValue);
  const fraction = (selected - minimumValue) / (maximumValue - minimumValue);
  const thumbSize = sizes.icon;
  const travel = Math.max(0, width - thumbSize);

  useEffect(() => {
    if (selected !== value) {
      onValueChange(selected);
    }
  }, [value, selected, onValueChange]);

  function notify(next: number) {
    if (lastNotified.current !== next) {
      lastNotified.current = next;
      onValueChange(next);
    }
  }

  function updateFromTouch(event: GestureResponderEvent) {
    if (travel <= 0) return;

    notify(
      sliderValueAtPosition(
        event.nativeEvent.pageX - origin.current - thumbSize / 2,
        travel,
        minimumValue,
        maximumValue
      )
    );
  }

  return (
    <View className="self-stretch">
      <View
        accessible
        accessibilityRole="adjustable"
        accessibilityLabel={accessibilityLabel}
        accessibilityValue={{
          min: minimumValue,
          max: maximumValue,
          now: selected,
        }}
        accessibilityActions={[{ name: 'increment' }, { name: 'decrement' }]}
        onAccessibilityAction={({ nativeEvent }) => {
          if (!['increment', 'decrement'].includes(nativeEvent.actionName))
            return;
          const direction = nativeEvent.actionName === 'increment' ? 1 : -1;
          lastNotified.current = null;
          notify(
            clampSliderValue(selected + direction, minimumValue, maximumValue)
          );
        }}
        className="h-button-height justify-center"
        onLayout={({ nativeEvent }) => setWidth(nativeEvent.layout.width)}
        onStartShouldSetResponder={() => travel > 0}
        onMoveShouldSetResponder={() => travel > 0}
        onResponderGrant={(event) => {
          origin.current =
            event.nativeEvent.pageX - event.nativeEvent.locationX;
          lastNotified.current = null;
          updateFromTouch(event);
        }}
        onResponderMove={updateFromTouch}
        onResponderRelease={updateFromTouch}
        onResponderTerminationRequest={() => false}
      >
        <View pointerEvents="none" style={{ marginHorizontal: thumbSize / 2 }}>
          <View className="h-progress-track-height overflow-hidden rounded-pill bg-progress-track">
            <View
              className="h-full rounded-pill bg-primary"
              style={{ width: `${fraction * 100}%` }}
            />
          </View>
        </View>
        <View
          pointerEvents="none"
          className="absolute h-icon w-icon rounded-pill bg-primary"
          style={{ left: fraction * travel }}
        />
      </View>
      <View
        className="mt-2 flex-row justify-between"
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
      >
        <Text className="font-inter text-bodySmall text-text-secondary">
          {minimumLabel}
        </Text>
        <Text className="font-inter text-bodySmall text-text-secondary">
          {maximumLabel}
        </Text>
      </View>
    </View>
  );
}
