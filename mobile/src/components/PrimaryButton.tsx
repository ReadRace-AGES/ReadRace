import { useState, type ComponentType } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radius, shadows, sizes, spacing, textStyles } from '@/theme';

export type PrimaryButtonIconProps = { size: number; color: string };

export type PrimaryButtonProps = {
  label: string;
  onPress: () => void;
  /** `inverse` e `outlineInverse` são para fundo escuro (tela de entrada). */
  variant?: 'filled' | 'outline' | 'inverse' | 'outlineInverse';
  icon?: ComponentType<PrimaryButtonIconProps>;
  disabled?: boolean;
  loading?: boolean;
};

export function PrimaryButton({
  label,
  onPress,
  variant = 'filled',
  icon: Icon,
  disabled = false,
  loading = false,
}: PrimaryButtonProps) {
  const [pressed, setPressed] = useState(false);
  const inactive = disabled || loading;
  const filled = variant === 'filled';
  const inverse = variant === 'inverse';
  const outlineInverse = variant === 'outlineInverse';
  const hasIcon = Boolean(Icon);

  const backgroundStyle = filled || inverse
    ? inactive
      ? styles.filledInactive
      : inverse
        ? styles.inverseActive
        : styles.filledActive
    : inactive
      ? styles.outlineInactive
      : outlineInverse
        ? styles.outlineInverse
        : [styles.outlineBase, hasIcon ? styles.outlineIcon : styles.outlineNoIcon];
  const contentColor = inactive
    ? colors.textMuted
    : filled || outlineInverse
      ? colors.textInverse
      : inverse || hasIcon
        ? colors.primary
        : colors.text;
  const textColorStyle = inactive
    ? styles.textInactive
    : filled
      ? styles.textFilledActive
      : inverse || outlineInverse
        ? { color: contentColor }
        : hasIcon
          ? styles.textOutlineIcon
          : styles.textOutlineNoIcon;

  function handlePress() {
    if (inactive) return;
    onPress();
  }

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: inactive, busy: loading }}
      disabled={inactive}
      onPress={handlePress}
      onPressIn={() => setPressed(true)}
      onPressOut={() => setPressed(false)}
      style={[
        styles.base,
        backgroundStyle,
        pressed && !inactive && styles.pressed,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={contentColor} />
      ) : (
        <>
          {Icon && (
            <View style={styles.iconWrap}>
              <Icon size={sizes.iconSmall} color={contentColor} />
            </View>
          )}
          <Text style={[textStyles.button, styles.text, textColorStyle]}>
            {label}
          </Text>
        </>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[2],
    minHeight: sizes.buttonHeight,
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[4],
    borderRadius: radius.pill,
  },
  filledActive: { backgroundColor: colors.primary, ...shadows.button },
  filledInactive: { backgroundColor: colors.surfaceDisabled },
  inverseActive: { backgroundColor: colors.surface },
  outlineInverse: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: colors.textInverse,
  },
  outlineBase: { backgroundColor: colors.surface, borderWidth: sizes.borderWidth },
  outlineIcon: { borderColor: colors.primary },
  outlineNoIcon: { borderColor: colors.borderStrong },
  outlineInactive: {
    backgroundColor: colors.surfaceDisabled,
    borderWidth: sizes.borderWidth,
    borderColor: colors.surfaceDisabled,
  },
  pressed: { opacity: 0.8 },
  iconWrap: { flexShrink: 0 },
  text: { flexShrink: 1, textAlign: 'center' },
  textFilledActive: { color: colors.textInverse },
  textInactive: { color: colors.textMuted },
  textOutlineIcon: { color: colors.primary },
  textOutlineNoIcon: { color: colors.text },
});

export default PrimaryButton;
