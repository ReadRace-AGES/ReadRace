import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAuth } from '@/auth/AuthProvider';
import { authHabilitada } from '@/auth/config';
import { BottomSheet } from '@/components/BottomSheet';
import { useToastContext } from '@/components/toast-provider';
import { colors, sizes, spacing, textStyles } from '@/theme';

type Props = {
  visible: boolean;
  onClose: () => void;
};

export function ConfiguracoesGeraisSheet({ visible, onClose }: Props) {
  const { sair } = useAuth();
  const { showToast } = useToastContext();

  // O modal fica por cima do toast: fecha o painel antes de avisar.
  function avisarEmDesenvolvimento() {
    onClose();
    showToast();
  }

  function desconectar() {
    // Sem Cognito configurado (modo seed) não há sessão para encerrar.
    if (!authHabilitada) {
      avisarEmDesenvolvimento();
      return;
    }
    onClose();
    // Ao encerrar a sessão, o app volta sozinho para o login (Stack.Protected).
    void sair();
  }

  return (
    <BottomSheet
      visible={visible}
      onClose={onClose}
      accessibilityLabel="Configurações gerais"
    >
      <View style={styles.conteudo}>
        <Text style={styles.titulo}>Configurações gerais</Text>
        <Item label="Notificações" onPress={avisarEmDesenvolvimento} />
        <Item label="Fale conosco" onPress={avisarEmDesenvolvimento} />
        <Item label="Desconectar" onPress={desconectar} perigo />
      </View>
    </BottomSheet>
  );
}

function Item({
  label,
  onPress,
  perigo = false,
}: {
  label: string;
  onPress: () => void;
  perigo?: boolean;
}) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.item}>
      <Text style={[styles.itemTexto, perigo && styles.itemPerigo]}>
        {label}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  conteudo: { gap: spacing[2], paddingBottom: spacing[4] },
  titulo: {
    ...textStyles.h3,
    color: colors.text,
    marginBottom: spacing[2],
  },
  item: {
    paddingVertical: spacing[3],
    borderBottomWidth: sizes.borderWidth,
    borderBottomColor: colors.border,
  },
  itemTexto: { ...textStyles.body, color: colors.text },
  itemPerigo: { color: colors.danger },
});