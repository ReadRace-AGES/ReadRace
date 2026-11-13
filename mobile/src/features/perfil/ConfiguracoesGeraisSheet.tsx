import { Pressable, StyleSheet, Text, View } from 'react-native';

import { useAuth } from '@/auth/AuthProvider';
import { authHabilitada } from '@/auth/config';
import { BottomSheet } from '@/components/BottomSheet';
import { LogoutIcon } from '@/components/icons/LogoutIcon';
import { useToastContext } from '@/components/toast-provider';
import { colors, sizes, spacing, textStyles } from '@/theme';

type Props = {
  visible: boolean;
  onClose: () => void;
};

export function ConfiguracoesGeraisSheet({ visible, onClose }: Props) {
  const { sair } = useAuth();
  const { showToast } = useToastContext();

  function desconectar() {
    onClose();
    // Sem Cognito configurado (modo seed) não há sessão para encerrar.
    // O modal fica por cima do toast: fecha o painel antes de avisar.
    if (!authHabilitada) {
      showToast();
      return;
    }
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
        <View style={styles.cabecalho}>
          <Text style={styles.titulo}>Configurações gerais</Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            onPress={onClose}
            hitSlop={spacing[3]}
            style={styles.botaoFechar}
          >
            <Text style={styles.textoFechar}>✕</Text>
          </Pressable>
        </View>
        <Pressable
          accessibilityRole="button"
          onPress={desconectar}
          style={styles.item}
        >
          <LogoutIcon />
          <Text style={styles.itemTexto}>Desconectar</Text>
        </Pressable>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  conteudo: { gap: spacing[2], paddingBottom: spacing[4] },
  cabecalho: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing[4],
    marginBottom: spacing[2],
  },
  titulo: {
    ...textStyles.h3,
    flex: 1,
    color: colors.text,
  },
  botaoFechar: {
    minWidth: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  textoFechar: { ...textStyles.h3, color: colors.text },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    paddingVertical: spacing[3],
    borderBottomWidth: sizes.borderWidth,
    borderBottomColor: colors.border,
  },
  itemTexto: { ...textStyles.body, color: colors.danger },
});
