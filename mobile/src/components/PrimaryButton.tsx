import { useState, type ComponentType } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radius, shadows, sizes, spacing, textStyles } from '@/theme';

export type PrimaryButtonIconProps = { size: number; color: string };

export type PrimaryButtonProps = {
  label: string;
  onPress: () => void;
  variant?: 'filled' | 'outline';
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
  const hasIcon = Boolean(Icon);

  const backgroundStyle = filled
    ? inactive
      ? styles.filledInactive
      : styles.filledActive
    : inactive
      ? styles.outlineInactive
      : [styles.outlineBase, hasIcon ? styles.outlineIcon : styles.outlineNoIcon];
  const textColorStyle = inactive
    ? styles.textInactive
    : filled
      ? styles.textFilledActive
      : hasIcon
        ? styles.textOutlineIcon
        : styles.textOutlineNoIcon;
  const contentColor = inactive
    ? colors.textMuted
    : filled
      ? colors.textInverse
      : hasIcon
        ? colors.primary
        : colors.text;

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
