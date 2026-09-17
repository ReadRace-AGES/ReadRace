import { useState } from 'react';
import { View, TextInput, TextInputProps, Platform } from 'react-native';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { colors, sizes, radius, spacing, typography, shadows } from '@/theme';

type SearchVariant = 'rounded' | 'outlined' | 'tinted';

export interface SearchInputProps
  extends Omit<TextInputProps, 'onChangeText' | 'value' | 'editable' | 'multiline' | 'numberOfLines'> {
  placeholder: string;
  value?: string;
  onChangeText?: (text: string) => void;
  disabled?: boolean;
  variant?: SearchVariant;
  className?: string; 
}

export function SearchInput({
  placeholder,
  value,
  onChangeText,
  disabled = false,
  variant = 'rounded',
  className = '',
  ...rest
}: SearchInputProps) {
  const [internalValue, setInternalValue] = useState('');

  const currentValue = value !== undefined ? value : internalValue;

  const handleChangeText = (text: string) => {
    if (value === undefined) {
      setInternalValue(text);
    }
    onChangeText?.(text);
  };

  const getVariantStyles = () => {
    let bgColor, borderColor, iconColor, borderRadius;

    switch (variant) {
      case 'outlined':
        bgColor = disabled ? colors.surfaceDisabled : colors.surface;
        borderColor = disabled ? colors.border : colors.borderStrong;
        iconColor = disabled ? colors.textMuted : colors.primarySoft;
        borderRadius = radius.md;
        break;
      case 'tinted':
        bgColor = disabled ? colors.surfaceDisabled : colors.surface;
        borderColor = disabled ? colors.border : colors.surfacePinkStrong;
        iconColor = disabled ? colors.textMuted : colors.primarySoft;
        borderRadius = radius.sm;
        break;
      case 'rounded':
      default:
        bgColor = disabled ? colors.surfaceDisabled : colors.surface;
        borderColor = disabled ? colors.border : colors.inputBorder;
        iconColor = disabled ? colors.textMuted : colors.primary;
        borderRadius = radius.pill;
        break;
    }

    return { bgColor, borderColor, iconColor, borderRadius };
  };

  const { bgColor, borderColor, iconColor, borderRadius } = getVariantStyles();

  return (
    <View
      className={`flex-row items-center ${className}`}
      style={{
        height: sizes.searchInputHeight,
        paddingHorizontal: spacing[4],
        gap: spacing[3],
        backgroundColor: bgColor,
        borderColor: borderColor,
        borderWidth: sizes.borderWidth,
        borderRadius: borderRadius,
        ...shadows.input,
      }}
    >

      <SearchIcon size={sizes.iconSmall} color={iconColor} />

      <TextInput
        {...rest}
        className={`flex-1 h-full ${Platform.OS === 'android' ? 'py-0' : ''}`}
        style={{
          color: disabled ? colors.textMuted : colors.text,
          fontFamily: typography.fontFamily.regular,
          fontSize: typography.fontSize.body,
          outlineStyle: 'solid',
          outlineWidth: 0,
        }}
        placeholder={placeholder}
        placeholderTextColor={colors.textSecondary}
        value={currentValue}
        onChangeText={handleChangeText}
        editable={!disabled}
        multiline={false}
        numberOfLines={1}
        autoCorrect={false}
        autoCapitalize="none"
      />
    </View>
  );
}