import { useState } from 'react';
import { View, TextInput, TextInputProps, Platform } from 'react-native';
import {SearchIcon} from '@/components/icons/SearchIcon';

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
    switch (variant) {
      case 'outlined':
        return {
          containerClass: disabled 
            ? 'bg-[#FFFFFF] border-[#D1D5DB] rounded-md' 
            : 'bg-[#FFFFFF] border-[#877273] rounded-md',
          iconColor: disabled ? '#A0A0A0' : '#6B5E5F',
        };
      case 'tinted':
        return {
          containerClass: disabled 
            ? 'bg-[#FCFAFA] border-[#D1D5DB] rounded-sm' 
            : 'bg-[#FCFAFA] border-[#DAC1C2] rounded-sm',
          iconColor: disabled ? '#A0A0A0' : '#6B5E5F',
        };
      case 'rounded':
      default:
        return {
          containerClass: disabled 
            ? 'bg-[#FFFFFF] border-[#D1D5DB] rounded-[999px]' 
            : 'bg-[#F9F9F9] border-[#E1E4E8] rounded-[999px]',
          iconColor: disabled ? '#A0A0A0' : '#732634',
        };
    }
  };

  const { containerClass, iconColor } = getVariantStyles();

  return (
    <View
      className={`h-[48] flex-row items-center px-4 border gap-3 ${containerClass} ${className}`}
      style={{
        shadowColor: '#000000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.05,
        shadowRadius: 2,
        elevation: 2,
      }}
    >
      <SearchIcon size={18} color={iconColor} />

      <TextInput
        {...rest}
        className={`flex-1 h-full text-[16px] ${Platform.OS === 'android' ? 'py-0' : ''} ${disabled ? 'text-gray-400' : 'text-[#374151]'}`}
        placeholder={placeholder}
        placeholderTextColor= {disabled ? "#888888" : "#505662"}
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
