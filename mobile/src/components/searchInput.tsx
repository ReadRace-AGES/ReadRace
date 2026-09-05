import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  TextInput,
  TextInputProps,
  Platform,
  StyleProp,
  ViewStyle,
} from 'react-native';
import Svg, { Path } from 'react-native-svg';

type SearchVariant = 'default' | 'challenge' | 'book';

export interface SearchInputProps extends Omit<TextInputProps, 'onChangeText' | 'value' | 'style'> {
  placeholder: string;
  value?: string;
  onChangeText?: (text: string) => void;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  variant?: SearchVariant; 
}

export const SearchInput: React.FC<SearchInputProps> = ({
  placeholder,
  value,
  onChangeText,
  disabled = false,
  variant = 'default',
  style,
  ...rest
}) => {
  const [internalValue, setInternalValue] = useState('');
  
  const currentValue = value !== undefined ? value : internalValue;

  const handleChangeText = (text: string) => {
    if (value === undefined) {
      setInternalValue(text);
    }
    onChangeText?.(text);
  };

  // Define as cores dinâmicas baseadas na variação escolhida
  const getVariantStyles = () => {
    switch (variant) {
      case 'challenge':
        return {
          containerStyle: styles.containerChallenge,
          iconColor: '#6B5E5F', 
        };
      case 'book':
        return {
          containerStyle: styles.containerBook,
          iconColor: '#6B5E5F', 
        };
      case 'default':
      default:
        return {
          containerStyle: styles.containerDefault,
          iconColor: '#732634', 
        };
    }
  };

  const { containerStyle, iconColor } = getVariantStyles();
  const finalIconColor = disabled ? '#A0A0A0' : iconColor;

  return (
    <View style={[
      styles.inputContainer, 
      containerStyle, 
      disabled && styles.inputContainerDisabled, 
      style
    ]}>
      <Svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <Path 
          d="M16.6 18L10.3 11.7C9.8 12.1 9.225 12.4167 8.575 12.65C7.925 12.8833 7.23333 13 6.5 13C4.68333 13 3.14583 12.3708 1.8875 11.1125C0.629167 9.85417 0 8.31667 0 6.5C0 4.68333 0.629167 3.14583 1.8875 1.8875C3.14583 0.629167 4.68333 0 6.5 0C8.31667 0 9.85417 0.629167 11.1125 1.8875C12.3708 3.14583 13 4.68333 13 6.5C13 7.23333 12.8833 7.925 12.65 8.575C12.4167 9.225 12.1 9.8 11.7 10.3L18 16.6L16.6 18ZM6.5 11C7.75 11 8.8125 10.5625 9.6875 9.6875C10.5625 8.8125 11 7.75 11 6.5C11 5.25 10.5625 4.1875 9.6875 3.3125C8.8125 2.4375 7.75 2 6.5 2C5.25 2 4.1875 2.4375 3.3125 3.3125C2.4375 4.1875 2 5.25 2 6.5C2 7.75 2.4375 8.8125 3.3125 9.6875C4.1875 10.5625 5.25 11 6.5 11Z" 
          fill={finalIconColor} 
        />
      </Svg>

      <TextInput
        style={[styles.textInput, disabled && styles.textInputDisabled]}
        placeholder={placeholder}
        placeholderTextColor="#78808B"
        value={currentValue}
        onChangeText={handleChangeText}
        editable={!disabled}
        selectTextOnFocus={!disabled}
        multiline={false}
        numberOfLines={1}
        autoCorrect={false}
        autoCapitalize="none"
        {...rest}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 48,
    borderRadius: 9999, 
    paddingHorizontal: 16,
    borderWidth: 1,
    gap: 12,

    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2, 
  },
  
  containerDefault: {
    backgroundColor: '#F9F9F9',
    borderColor: '#E1E4E8',
  },
  
  containerChallenge: {
    backgroundColor: '#FFFFFF',
    borderColor: '#877273',
    borderRadius: 12,
  },
  
  containerBook: {
    backgroundColor: '#FCFAFA',
    borderColor: '#DAC1C2', 
    borderRadius: 8,
  },

  inputContainerDisabled: {
    backgroundColor: '#F3F4F6',
    borderColor: '#E5E7EB',
  },
  textInput: {
    flex: 1, 
    height: '100%',
    fontSize: 16,
    color: '#374151', 
    paddingVertical: Platform.OS === 'android' ? 0 : undefined, 
  },
  textInputDisabled: {
    color: '#9CA3AF',
  },
});