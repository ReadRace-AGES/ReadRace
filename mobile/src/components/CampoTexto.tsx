import {
  StyleSheet,
  Text,
  TextInput,
  View,
  type TextInputProps,
} from 'react-native';

import { colors, radius, spacing, textStyles } from '@/theme';

type CampoTextoProps = TextInputProps & {
  rotulo: string;
  /** Texto cinza embaixo do campo (ex.: regra da senha). */
  ajuda?: string;
  /** Borda e texto vermelhos embaixo do campo. Tem prioridade sobre `ajuda`. */
  erro?: string | null;
};

/** Rótulo e campo, como os formulários de login e cadastro do Figma. */
export function CampoTexto({
  rotulo,
  ajuda,
  erro,
  style,
  editable = true,
  ...props
}: CampoTextoProps) {
  const mensagem = erro ?? ajuda;
  return (
    <View style={styles.bloco}>
      <Text style={[textStyles.body, styles.rotulo]}>{rotulo}</Text>
      <TextInput
        accessibilityLabel={rotulo}
        accessibilityHint={mensagem ?? undefined}
        placeholderTextColor={colors.textMuted}
        editable={editable}
        style={[
          styles.campo,
          erro && styles.comErro,
          !editable && styles.bloqueado,
          style,
        ]}
        {...props}
      />
      {mensagem ? (
        <Text
          style={[
            textStyles.caption,
            erro ? styles.textoErro : styles.textoAjuda,
          ]}
        >
          {mensagem}
        </Text>
      ) : null}
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
  comErro: { borderColor: colors.danger },
  bloqueado: { opacity: 0.6 },
  textoAjuda: { color: colors.textSecondary },
  textoErro: { color: colors.danger },
});

export default CampoTexto;
