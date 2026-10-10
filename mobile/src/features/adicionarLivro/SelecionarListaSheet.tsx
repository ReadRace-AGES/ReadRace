import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { ApiError } from '@/api/client';
import { BookCover } from '@/components/BookCover';
import { BottomSheet } from '@/components/BottomSheet';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing, textStyles } from '@/theme';

import {
  adicionarNaBiblioteca,
  type ListaDestino,
  type LivroAdicionado,
  type ResultadoCatalogo,
} from './api';

export type SelecionarListaSheetProps = {
  visible: boolean;
  livro: ResultadoCatalogo | null;
  onClose: () => void;
  onSuccess: (resultado: LivroAdicionado) => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível adicionar o livro. Tente novamente.';
}

/** Capa, título, autor, ISBN, páginas e os três botões de lista (#155). */
export function SelecionarListaSheet({
  visible,
  livro,
  onClose,
  onSuccess,
}: SelecionarListaSheetProps) {
  const [listaEmAndamento, setListaEmAndamento] = useState<ListaDestino | null>(
    null
  );
  const [error, setError] = useState<string | null>(null);
  const submitting = useRef(false);

  async function selecionar(lista: ListaDestino) {
    if (submitting.current || !livro) return;

    submitting.current = true;
    setListaEmAndamento(lista);
    setError(null);
    try {
      const resultado = await adicionarNaBiblioteca(livro.volumeId, lista);
      onSuccess(resultado);
    } catch (erro) {
      setError(mensagemDe(erro));
    } finally {
      submitting.current = false;
      setListaEmAndamento(null);
    }
  }

  if (!livro) return null;

  const carregando = listaEmAndamento !== null;

  return (
    <BottomSheet
      visible={visible}
      dismissible={!carregando}
      onClose={onClose}
      accessibilityLabel="Adicionar livro"
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>Adicionar livro</Text>
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar"
            disabled={carregando}
            onPress={onClose}
            hitSlop={spacing[3]}
            style={styles.closeButton}
          >
            <Text style={styles.closeText}>✕</Text>
          </Pressable>
        </View>

        <View style={styles.resumo}>
          <BookCover
            source={livro.capaUrl}
            size="thumbnail"
            accessibilityLabel={`Capa de ${livro.titulo}`}
          />
          <View style={styles.info}>
            <Text style={styles.livroTitulo} numberOfLines={2}>
              {livro.titulo}
            </Text>
            {!!livro.autor && (
              <Text style={styles.autor} numberOfLines={1}>
                {livro.autor}
              </Text>
            )}
            <Text style={styles.meta}>ISBN: {livro.isbn}</Text>
            <Text style={styles.meta}>{livro.paginas} páginas</Text>
          </View>
        </View>

        {error && (
          <Text
            accessibilityRole="alert"
            accessibilityLiveRegion="polite"
            style={styles.error}
          >
            {error}
          </Text>
        )}

        <View style={styles.actions}>
          <PrimaryButton
            label="Lido"
            loading={listaEmAndamento === 'lido'}
            disabled={carregando && listaEmAndamento !== 'lido'}
            onPress={() => selecionar('lido')}
          />
          <PrimaryButton
            label="Desejos"
            variant="outline"
            loading={listaEmAndamento === 'desejo'}
            disabled={carregando && listaEmAndamento !== 'desejo'}
            onPress={() => selecionar('desejo')}
          />
          <PrimaryButton
            label="Favoritos"
            variant="outline"
            loading={listaEmAndamento === 'favorito'}
            disabled={carregando && listaEmAndamento !== 'favorito'}
            onPress={() => selecionar('favorito')}
          />
        </View>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing[5] },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: spacing[4],
  },
  title: { ...textStyles.h1, color: colors.primary, flex: 1 },
  closeButton: {
    minWidth: spacing[8],
    minHeight: spacing[8],
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeText: { ...textStyles.h1, color: colors.text },
  resumo: { flexDirection: 'row', gap: spacing[4] },
  info: { flex: 1, minWidth: 0, gap: spacing[1], justifyContent: 'center' },
  livroTitulo: { ...textStyles.bodyStrong, color: colors.text },
  autor: { ...textStyles.bodySmall, color: colors.textSecondary },
  meta: { ...textStyles.caption, color: colors.textSecondary },
  error: {
    ...textStyles.bodySmall,
    color: colors.accent,
    textAlign: 'center',
  },
  actions: { gap: spacing[2] },
});
