import {
  Inter_400Regular,
  Inter_600SemiBold,
  Inter_700Bold,
  Inter_800ExtraBold,
  useFonts,
} from '@expo-google-fonts/inter';
import type { TextStyle } from 'react-native';

import tokens from './tokens';

export const { colors, typography, spacing, radius, shadows, sizes, gradients } = tokens;

export type ColorToken = keyof typeof colors;
export type FontSizeToken = keyof typeof typography.fontSize;
export type SpacingToken = keyof typeof spacing;
export type RadiusToken = keyof typeof radius;

type Weight = keyof typeof typography.fontFamily;
type Leading = keyof typeof typography.lineHeight;

function text(size: FontSizeToken, weight: Weight, leading: Leading = 'normal'): TextStyle {
  const fontSize = typography.fontSize[size];
  return {
    fontFamily: typography.fontFamily[weight],
    fontSize,
    lineHeight: Math.round(fontSize * typography.lineHeight[leading]),
    letterSpacing: Number((fontSize * typography.letterSpacingRatio).toFixed(3)),
  };
}

export const textStyles = {
  display: text('display', 'bold', 'tight'),
  // Titulo dentro do cabecalho vermelho — `menu - comunidades` 3-7, `desafios` 3-8.
  headerTitle: text('h1', 'extrabold', 'heading'),
  h1: text('h1', 'bold'),
  h2: text('h2', 'bold'),
  h3: text('h3', 'semibold'),
  bodyStrong: text('body', 'bold'),
  body: text('body', 'regular'),
  button: text('body', 'bold'),
  bodySmallStrong: text('bodySmall', 'bold'),
  bodySmall: text('bodySmall', 'regular'),
  captionStrong: text('caption', 'bold'),
  caption: text('caption', 'regular'),
  microStrong: text('micro', 'bold'),
  micro: text('micro', 'regular'),
} satisfies Record<string, TextStyle>;

export type TextStyleToken = keyof typeof textStyles;

export function useAppFonts() {
  return useFonts({
    [typography.fontFamily.regular]: Inter_400Regular,
    [typography.fontFamily.semibold]: Inter_600SemiBold,
    [typography.fontFamily.bold]: Inter_700Bold,
    [typography.fontFamily.extrabold]: Inter_800ExtraBold,
  });
}

export const theme = {
  colors,
  typography,
  textStyles,
  spacing,
  radius,
  shadows,
  sizes,
  gradients,
};
export type Theme = typeof theme;

export default theme;
