import { Children, Fragment, isValidElement, type ReactNode } from 'react';
import {
  Pressable,
  StyleSheet,
  View,
  type AccessibilityState,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { colors, radius, shadows, spacing } from '@/theme';

export type CardProps = {
  children?: ReactNode;
  onPress?: () => void;
  accessibilityLabel?: string;
  accessibilityState?: AccessibilityState;
  /** Layout externo, como margem e largura. O espaçamento interno vem do tema. */
  style?: StyleProp<ViewStyle>;
  /** Estilo da superfície interna, preservando o recorte e a sombra externa. */
  surfaceStyle?: StyleProp<ViewStyle>;
  testID?: string;
};

function temConteudo(children: ReactNode): boolean {
  return Children.toArray(children).some((child) => {
    if (typeof child === 'string') return child.trim().length > 0;
    if (
      isValidElement<{ children?: ReactNode }>(child) &&
      child.type === Fragment
    ) {
      return temConteudo(child.props.children);
    }
    return true;
  });
}

/** Container da #17. Conteúdo, navegação e espaçamento entre cards pertencem à tela. */
export function Card({
  children,
  onPress,
  accessibilityLabel,
  accessibilityState,
  style,
  surfaceStyle,
  testID,
}: CardProps) {
  if (!temConteudo(children)) return null;

  // A sombra fica fora do recorte para não ser cortada pelo overflow no iOS.
  return (
    <View style={[styles.shadow, style]} testID={testID}>
      {onPress ? (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={accessibilityLabel}
          accessibilityState={accessibilityState}
          onPress={onPress}
          style={({ pressed }) => [
            styles.surface,
            surfaceStyle,
            pressed && styles.pressed,
          ]}
        >
          {children}
        </Pressable>
      ) : (
        <View style={[styles.surface, surfaceStyle]}>{children}</View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  shadow: {
    ...shadows.floating,
    alignSelf: 'stretch',
    minWidth: 0,
    flexShrink: 1,
    backgroundColor: colors.surfaceMuted,
    borderRadius: radius.lg,
  },
  surface: {
    padding: spacing[4],
    borderRadius: radius.lg,
    backgroundColor: colors.surfaceMuted,
    overflow: 'hidden',
  },
  pressed: {
    backgroundColor: colors.surfaceAlt,
  },
});

export default Card;
