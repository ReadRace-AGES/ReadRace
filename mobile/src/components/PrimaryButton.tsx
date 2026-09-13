import type { ComponentType } from 'react';
import { ActivityIndicator, Pressable, Text, View } from 'react-native';

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
  const inactive = disabled || loading;
  const filled = variant === 'filled';
  const hasIcon = Boolean(Icon);

  const outlineColor = hasIcon ? colors.primary : colors.text;
  const outlineBorderColor = hasIcon ? colors.primary : colors.borderStrong;

  const backgroundColor = inactive
    ? colors.surfaceDisabled
    : filled
      ? colors.primary
      : colors.surface;
  const contentColor = inactive
    ? colors.textMuted
    : filled
      ? colors.textInverse
      : outlineColor;

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
      style={({ pressed }) => [
        {
          flexDirection: 'row',
          alignItems: 'center',
          justifyContent: 'center',
          gap: spacing[2],
          height: sizes.buttonHeight,
          paddingHorizontal: spacing[4],
          borderRadius: radius.pill,
          backgroundColor,
        },
        !filled && {
          borderWidth: sizes.borderWidth,
          borderColor: inactive ? colors.surfaceDisabled : outlineBorderColor,
        },
        filled && !inactive && shadows.button,
        pressed && !inactive && { opacity: 0.8 },
      ]}
    >
      {loading ? (
        <ActivityIndicator color={contentColor} />
      ) : (
        <>
          {Icon && (
            <View style={{ flexShrink: 0 }}>
              <Icon size={sizes.iconSmall} color={contentColor} />
            </View>
          )}
          <Text
            numberOfLines={1}
            ellipsizeMode="tail"
            style={[textStyles.button, { color: contentColor, flexShrink: 1 }]}
          >
            {label}
          </Text>
        </>
      )}
    </Pressable>
  );
}

export default PrimaryButton;
