import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { ApiError } from '@/api/client';
import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { LockIcon } from '@/components/icons/LockIcon';
import { PawIcon } from '@/components/icons/PawIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';
import { escolherMascote, type Mascote } from './api';
import { arteDoMascote, PROPORCAO_ARTE, SILHUETA } from './imagens';
import { useMascotes } from './useMascotes';

function CartaMascote({
  mascote,
  selecionado,
  onSelecionar,
}: {
  mascote: Mascote;
  selecionado: boolean;
  onSelecionar: () => void;
}) {
  const bloqueado = !mascote.desbloqueado;
  return (
    <View style={styles.cell}>
      <Pressable
        cssInterop={false}
        accessibilityRole="button"
        accessibilityLabel={
          bloqueado
            ? `Mascote bloqueado, desbloqueia no nível ${mascote.nivelMinimo}`
            : mascote.nome
        }
        accessibilityState={{ selected: selecionado, disabled: bloqueado }}
        disabled={bloqueado}
        onPress={onSelecionar}
        style={({ pressed }) => [
          styles.carta,
          selecionado && styles.cartaSelecionada,
          pressed && styles.pressed,
        ]}
      >
        <View style={styles.arte}>
          <Image
            source={bloqueado ? SILHUETA : arteDoMascote(mascote.imagem)}
            style={StyleSheet.absoluteFill}
            contentFit="contain"
            contentPosition="bottom"
            accessible={false}
          />
          {bloqueado && (
            <View style={styles.cadeado}>
              <LockIcon size={sizes.icon * 2} color={colors.text} />
            </View>
          )}
        </View>
        <Text numberOfLines={1} style={styles.nome}>
          {bloqueado ? '???' : mascote.nome}
        </Text>
        {bloqueado && (
          <Text numberOfLines={1} style={styles.nivel}>
            {`Desbloqueia no nível ${mascote.nivelMinimo}`}
          </Text>
        )}
      </Pressable>
    </View>
  );
}

function Grade({
  mascotes,
  aoConcluir,
}: {
  mascotes: Mascote[];
  aoConcluir: () => void;
}) {
  const { showErrorToast } = useToastContext();
  const escolhido = mascotes.find((m) => m.escolhido)?.id;
  const [selecionado, setSelecionado] = useState(escolhido);
  const [salvando, setSalvando] = useState(false);

  async function concluir() {
    if (selecionado === undefined || selecionado === escolhido) {
      aoConcluir();
      return;
    }
    setSalvando(true);
    try {
      await escolherMascote(selecionado);
      aoConcluir();
    } catch (erro) {
      // A seleção continua na tela para tentar de novo.
      showErrorToast(
        erro instanceof ApiError
          ? erro.message
          : 'Não foi possível salvar o mascote. Tente novamente.'
      );
    } finally {
      setSalvando(false);
    }
  }

  return (
    <>
      <ScrollView style={styles.lista} contentContainerStyle={styles.grade}>
        {mascotes.map((mascote) => (
          <CartaMascote
            key={mascote.id}
            mascote={mascote}
            selecionado={mascote.id === selecionado}
            onSelecionar={() => setSelecionado(mascote.id)}
          />
        ))}
      </ScrollView>
      <View style={styles.rodape}>
        <PrimaryButton label="Concluir" onPress={concluir} loading={salvando} />
      </View>
    </>
  );
}

export function MascotesScreen() {
  const router = useRouter();
  const { estado, recarregar } = useMascotes();
  const voltar = () =>
    router.canGoBack() ? router.back() : router.replace('/perfil');
  return (
    <View style={styles.screen}>
      <AppHeader
        compact
        title="Mascotes"
        subtitle="Personalize seu parceiro de leitura!"
        showBack
        onBackPress={voltar}
      />
      {estado.situacao === 'carregando' && (
        <ActivityIndicator
          style={styles.carregando}
          accessibilityLabel="Carregando mascotes"
          color={colors.primary}
        />
      )}
      {estado.situacao === 'erro' && (
        <EmptyState
          icon={PawIcon}
          message={estado.mensagem}
          action={
            <PrimaryButton label="Tentar novamente" onPress={recarregar} />
          }
        />
      )}
      {estado.situacao === 'sucesso' && (
        <Grade mascotes={estado.dados} aoConcluir={voltar} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  carregando: { padding: spacing[6] },
  lista: { flex: 1 },
  grade: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    rowGap: spacing[3],
    paddingTop: spacing[4],
    paddingHorizontal: spacing[3],
    paddingBottom: spacing[6],
  },
  cell: { width: '50%', paddingHorizontal: spacing[1] },
  carta: {
    gap: spacing[2],
    padding: spacing[2],
    borderRadius: radius.sm,
    borderWidth: sizes.borderWidth * 2,
    // Borda da cor do fundo: a carta não muda de tamanho ao ganhar o destaque.
    borderColor: colors.surfaceDisabled,
    backgroundColor: colors.surfaceDisabled,
  },
  cartaSelecionada: { borderColor: colors.accent },
  pressed: { backgroundColor: colors.surfacePink },
  arte: { width: '100%', aspectRatio: PROPORCAO_ARTE },
  cadeado: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  nome: {
    ...textStyles.captionStrong,
    color: colors.text,
    textAlign: 'center',
  },
  nivel: {
    ...textStyles.caption,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  rodape: {
    paddingHorizontal: spacing[5],
    paddingVertical: spacing[3],
    backgroundColor: colors.surface,
  },
});
