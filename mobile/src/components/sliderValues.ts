export function clampSliderValue(
  value: number,
  minimum: number,
  maximum: number
): number {
  return Math.min(maximum, Math.max(minimum, Math.round(value)));
}

export function sliderValueAtPosition(
  position: number,
  width: number,
  minimum: number,
  maximum: number
): number {
  const fraction = Math.min(1, Math.max(0, position / width));
  return clampSliderValue(
    minimum + fraction * (maximum - minimum),
    minimum,
    maximum
  );
}
