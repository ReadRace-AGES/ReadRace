import { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { router, useLocalSearchParams, type Href } from 'expo-router';

import { ListItem } from '@/components/ListItem';
import { PrimaryButton } from '@/components/PrimaryButton';
import { SearchInput } from '@/components/SearchInput';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import { useBiblioteca } from '@/features/biblioteca/useBiblioteca';
import type { LivroBiblioteca } from '@/features/biblioteca/api';
import { colors, radius, spacing, textStyles } from '@/theme';

const ABAS = ['Sua Estante', 'Lendo Agora'] as const;
type AbaBiblioteca = (typeof ABAS)[number];

function parametroUnico(valor?: string | string[]) {
  return Array.isArray(valor) ? valor[0] : valor;
}

function normalizar(texto: string) {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase('pt-BR');
}

export default function EscolherLivroScreen() {
  const { livroId: livroIdParam } = useLocalSearchParams<{
    livroId?: string | string[];
  }>();
  const [query, setQuery] = useState('');
  const [aba, setAba] = useState<AbaBiblioteca>(ABAS[0]);
  const [selectedId, setSelectedId] = useState<string | null>(
    parametroUnico(livroIdParam) || null
  );
  const { biblioteca, recarregar } = useBiblioteca();

  const books = useMemo(() => {
    if (biblioteca.situacao !== 'sucesso') {
      return [];
    }

    const listas = biblioteca.dados;
    if (aba === 'Lendo Agora') return listas.lendo.itens;

    const unicos = new Map<string, LivroBiblioteca>();
    for (const lista of [listas.favoritos, listas.lendo, listas.desejo, listas.lidos]) {
      for (const livro of lista.itens) unicos.set(livro.livroId, livro);
    }
    return [...unicos.values()];
  }, [aba, biblioteca]);

  const livrosFiltrados = useMemo(() => {
    const termo = normalizar(query.trim());
    if (!termo) return books;
    return books.filter((livro) =>
      normalizar(`${livro.titulo} ${livro.autor ?? ''}`).includes(termo)
    );
  }, [books, query]);

  const livroSelecionado = livrosFiltrados.find(
    (livro) => livro.livroId === selectedId
  );

  function confirmarLivro() {
    if (!livroSelecionado) return;
    router.dismissTo({
      pathname: '/desafiar-amigo',
      params: { livroSelecionado: JSON.stringify(livroSelecionado) },
    } as Href);
  }

  return (
    <View style={styles.screen}>
      <View style={styles.headerRow}>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Voltar"
          onPress={() => router.back()}
          style={styles.backButton}
        >
          <Text style={styles.backIcon}>←</Text>
        </Pressable>
        <Text style={styles.title}>Escolher Livro</Text>
      </View>

      <View style={styles.content}>
        <SearchInput
          placeholder="Buscar título ou autor..."
          value={query}
          onChangeText={setQuery}
          variant="rounded"
          className="w-full"
        />

        <View style={styles.libraryWrap}>
          <SegmentedTabs
            options={[...ABAS]}
            initialOption={ABAS[0]}
            onChange={(opcao) => setAba(opcao as AbaBiblioteca)}
          />
        </View>

        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContent}
          keyboardShouldPersistTaps="handled"
        >
          {biblioteca.situacao === 'carregando' && (
            <View style={styles.loadingRow}>
              <ActivityIndicator color={colors.primary} />
              <Text style={styles.loadingText}>Carregando sua biblioteca...</Text>
            </View>
          )}

          {biblioteca.situacao === 'erro' && (
            <View style={styles.errorState}>
              <Text accessibilityRole="alert" style={styles.emptyState}>
                {biblioteca.mensagem}
              </Text>
              <PrimaryButton
                label="Tentar de novo"
                variant="outline"
                onPress={recarregar}
              />
            </View>
          )}

          {biblioteca.situacao === 'sucesso' && livrosFiltrados.length === 0 && (
            <Text style={styles.emptyState}>
              {query.trim()
                ? 'Nenhum livro encontrado.'
                : aba === 'Lendo Agora'
                  ? 'Você não tem livros em leitura.'
                  : 'Sua biblioteca está vazia.'}
            </Text>
          )}

          {biblioteca.situacao === 'sucesso' && livrosFiltrados.map((book) => (
            <ListItem
              key={book.livroId}
              variant="book"
              title={book.titulo}
              subtitle={book.autor ?? 'Autor desconhecido'}
              imageUrl={book.capaUrl}
              selected={selectedId === book.livroId}
              onPress={() => setSelectedId(book.livroId)}
            />
          ))}
        </ScrollView>

        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Confirmar livro"
          onPress={confirmarLivro}
          style={[styles.confirmButton, !selectedId && styles.confirmButtonDisabled]}
          disabled={!livroSelecionado}
        >
          <Text style={styles.confirmText}>Confirmar Livro →</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#F3F3F3',
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingTop: 18,
    paddingBottom: 10,
    paddingHorizontal: 18,
    backgroundColor: '#F3F3F3',
  },
  backButton: {
    width: 28,
    height: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backIcon: {
    ...textStyles.h2,
    color: colors.text,
    lineHeight: 24,
  },
  title: {
    ...textStyles.h2,
    color: colors.text,
    fontWeight: '700',
    flex: 1,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
    paddingBottom: 20,
  },
  libraryWrap: {
    marginTop: 12,
    marginBottom: 10,
  },
  listContent: {
    gap: 12,
    paddingBottom: 16,
  },
  loadingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
  },
  loadingText: {
    ...textStyles.body,
    color: colors.textSecondary,
  },
  emptyState: {
    ...textStyles.body,
    color: colors.textSecondary,
    textAlign: 'center',
    paddingTop: 8,
    paddingBottom: 4,
  },
  errorState: {
    alignItems: 'center',
    gap: spacing[4],
  },
  confirmButton: {
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    minHeight: 52,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
    paddingHorizontal: spacing[4],
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.15,
    shadowRadius: 5,
    elevation: 4,
  },
  confirmButtonDisabled: {
    opacity: 0.6,
  },
  confirmText: {
    ...textStyles.button,
    color: colors.textInverse,
  },
});
