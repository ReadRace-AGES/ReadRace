import { StyleSheet, Text, TextInput, View, type TextInputProps } from 'react-native';

import { colors, radius, spacing, textStyles } from '@/theme';

type CampoTextoProps = TextInputProps & { rotulo: string };

/** Rótulo e campo, como os formulários de login e cadastro do Figma. */
export function CampoTexto({ rotulo, style, editable = true, ...props }: CampoTextoProps) {
  return (
    <View style={styles.bloco}>
      <Text style={[textStyles.body, styles.rotulo]}>{rotulo}</Text>
      <TextInput
        accessibilityLabel={rotulo}
        placeholderTextColor={colors.textMuted}
        editable={editable}
        style={[styles.campo, !editable && styles.bloqueado, style]}
        {...props}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  bloco: { gap: spacing[2] },
  rotulo: { color: colors.text },
  campo: {
    ...textStyles.body,
    // No Android, lineHeight em TextInput desalinha o texto do campo.
    lineHeight: undefined,
    minHeight: 48,
    paddingHorizontal: spacing[3],
    borderWidth: 1,
    borderColor: colors.inputBorder,
    borderRadius: radius.sm,
    backgroundColor: colors.surface,
    color: colors.text,
  },
  bloqueado: { opacity: 0.6 },
});

export default CampoTexto;
