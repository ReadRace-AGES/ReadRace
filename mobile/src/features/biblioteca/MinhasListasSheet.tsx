import { useEffect, useRef, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { BottomSheet } from '@/components/BottomSheet';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing, textStyles } from '@/theme';

import {
  atualizarStatusBiblioteca,
  desmarcarFavoritoBiblioteca,
  marcarFavoritoBiblioteca,
  removerDaBiblioteca,
  type BibliotecaItemResponse,
} from './api';

type StatusLeitura = BibliotecaItemResponse['statusLeitura'] | null;

export type MinhasListasSheetProps = {
  visible: boolean;
  livroId: string;
  titulo: string;
  statusLeitura: StatusLeitura;
  favorito: boolean;
  paginaMaxima: number;
  onClose: () => void;
  onSuccess: (resultado: BibliotecaItemResponse | null) => void;
};

const ERRO = 'Não foi possível atualizar suas listas. Tente novamente.';

export function MinhasListasSheet({
  visible,
  livroId,
  titulo,
  statusLeitura,
  favorito,
  paginaMaxima,
  onClose,
  onSuccess,
}: MinhasListasSheetProps) {
  const [status, setStatus] = useState(statusLeitura);
  const [isFavorito, setIsFavorito] = useState(favorito);
  const [loading, setLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);

  useEffect(() => {
    if (visible) {
      setStatus(statusLeitura);
      setIsFavorito(favorito);
      setError(null);
    }
  }, [favorito, statusLeitura, visible]);

  async function atualizar(chave: 'lido' | 'desejo' | 'favorito') {
    if (submitting.current) return;
    submitting.current = true;
    setLoading(chave);
    setError(null);
    try {
      const resultado =
        chave === 'favorito'
          ? isFavorito
            ? await desmarcarFavoritoBiblioteca(livroId)
            : await marcarFavoritoBiblioteca(livroId)
          : await atualizarStatusBiblioteca(livroId, chave);
      setStatus(resultado.statusLeitura);
      setIsFavorito(resultado.favorito);
      onSuccess(resultado);
      onClose();
    } catch {
      setError(ERRO);
    } finally {
      submitting.current = false;
      setLoading(null);
    }
  }

  function confirmarRemocao() {
    if (submitting.current) return;
    Alert.alert('Remover livro', `Remover ${titulo} da sua biblioteca?`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Remover',
        style: 'destructive',
        onPress: async () => {
          submitting.current = true;
          setLoading('remover');
          setError(null);
          try {
            await removerDaBiblioteca(livroId);
            onSuccess(null);
            onClose();
          } catch {
            setError(ERRO);
          } finally {
            submitting.current = false;
            setLoading(null);
          }
        },
      },
    ]);
  }

  const carregando = loading !== null;
  const statusManualIndisponivel = paginaMaxima > 0;

  return (
    <BottomSheet
      visible={visible}
      dismissible={!carregando}
      onClose={onClose}
      accessibilityLabel="Minhas listas"
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Minhas listas</Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={carregando}
            onPress={onClose}
            hitSlop={spacing[3]}
          >
            <Text style={styles.close}>✕</Text>
          </Pressable>
        </View>
        {error && (
          <Text accessibilityRole="alert" style={styles.error}>
            {error}
          </Text>
        )}
        <View style={styles.actions}>
          <PrimaryButton
            label="Lido"
            variant={status === 'lido' ? 'filled' : 'outline'}
            loading={loading === 'lido'}
            disabled={
              statusManualIndisponivel || (carregando && loading !== 'lido')
            }
            onPress={() => atualizar('lido')}
          />
          <PrimaryButton
            label="Desejos"
            variant={status === 'desejo' ? 'filled' : 'outline'}
            loading={loading === 'desejo'}
            disabled={
              statusManualIndisponivel || (carregando && loading !== 'desejo')
            }
            onPress={() => atualizar('desejo')}
          />
          <PrimaryButton
            label={isFavorito ? 'Favoritos' : 'Favoritos'}
            variant={isFavorito ? 'filled' : 'outline'}
            loading={loading === 'favorito'}
            disabled={carregando && loading !== 'favorito'}
            onPress={() => atualizar('favorito')}
          />
        </View>
        {paginaMaxima <= 0 && (
          <PrimaryButton
            label="Remover da biblioteca"
            variant="outline"
            loading={loading === 'remover'}
            disabled={carregando}
            onPress={confirmarRemocao}
          />
        )}
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing[5] },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  title: { ...textStyles.h1, color: colors.primary },
  close: { ...textStyles.h1, color: colors.text },
  actions: { gap: spacing[2] },
  error: { ...textStyles.bodySmall, color: colors.danger, textAlign: 'center' },
});
