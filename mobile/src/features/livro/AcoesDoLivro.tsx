import { StyleSheet, Text, View } from 'react-native';

import { BottomSheet } from '@/components/BottomSheet';
import { BookIcon } from '@/components/icons/BookIcon';
import { PencilIcon } from '@/components/icons/PencilIcon';
import { TrashIcon } from '@/components/icons/TrashIcon';
import { MenuSuspenso, type Ancora } from '@/components/MenuSuspenso';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, sizes, spacing, textStyles, typography } from '@/theme';

const COPY = {
  registrarProgresso: 'Registrar progresso',
  escreverPost: 'Escrever post sobre o livro',
  apagarPost: 'Apagar post',
  tituloConfirmacao: 'Apagar post?',
  descricaoConfirmacao:
    'O post e os comentários dele somem de "Posts sobre este livro". Não dá para desfazer.',
  confirmar: 'Apagar',
  cancelar: 'Cancelar',
} as const;

export function MenuDoLivro({
  ancora,
  onClose,
  onRegistrarProgresso,
  onEscreverPost,
}: {
  ancora: Ancora | null;
  onClose: () => void;
  onRegistrarProgresso: () => void;
  onEscreverPost: () => void;
}) {
  return (
    <MenuSuspenso
      ancora={ancora}
      accessibilityLabel="Ações do livro"
      onClose={onClose}
      itens={[
        {
          label: COPY.registrarProgresso,
          icone: <BookIcon size={sizes.icon} />,
          onPress: onRegistrarProgresso,
        },
        {
          label: COPY.escreverPost,
          icone: <PencilIcon size={sizes.icon} />,
          onPress: onEscreverPost,
        },
      ]}
    />
  );
}

export function MenuDoPost({
  ancora,
  onClose,
  onApagar,
}: {
  ancora: Ancora | null;
  onClose: () => void;
  onApagar: () => void;
}) {
  return (
    <MenuSuspenso
      ancora={ancora}
      accessibilityLabel="Ações do post"
      onClose={onClose}
      itens={[
        {
          label: COPY.apagarPost,
          icone: <TrashIcon size={sizes.icon} />,
          destrutivo: true,
          onPress: onApagar,
        },
      ]}
    />
  );
}

export function ConfirmarApagarPostSheet({
  visible,
  apagando,
  erro,
  onConfirmar,
  onClose,
}: {
  visible: boolean;
  apagando: boolean;
  erro: string | null;
  onConfirmar: () => void;
  onClose: () => void;
}) {
  return (
    <BottomSheet
      visible={visible}
      onClose={onClose}
      dismissible={!apagando}
      accessibilityLabel="Confirmar exclusão do post"
    >
      <View style={styles.conteudo}>
        <Text style={styles.titulo}>{COPY.tituloConfirmacao}</Text>
        <Text style={styles.descricao}>{COPY.descricaoConfirmacao}</Text>
        {erro && (
          <Text accessibilityRole="alert" style={styles.erro}>
            {erro}
          </Text>
        )}
        <PrimaryButton
          label={COPY.confirmar}
          loading={apagando}
          disabled={apagando}
          onPress={onConfirmar}
        />
        <PrimaryButton
          label={COPY.cancelar}
          variant="outline"
          disabled={apagando}
          onPress={onClose}
        />
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  conteudo: { paddingBottom: spacing[6], gap: spacing[4] },
  titulo: {
    ...textStyles.body,
    fontFamily: typography.fontFamily.bold,
    textAlign: 'center',
  },
  descricao: {
    ...textStyles.body,
    color: colors.textMuted,
    textAlign: 'center',
  },
  erro: {
    ...textStyles.bodySmall,
    color: colors.danger,
    textAlign: 'center',
  },
});
