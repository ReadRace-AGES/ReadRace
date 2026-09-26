import { useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { router } from 'expo-router';

import { ListItem } from '@/components/ListItem';
import { SearchInput } from '@/components/SearchInput';
import { useBusca } from '@/features/busca/useBusca';
import { colors, radius, spacing, textStyles } from '@/theme';

type BookOption = {
  id: string;
  title: string;
  author: string;
  genre: string;
  imageUrl: string | null;
};

export default function EscolherLivroScreen() {
  const [query, setQuery] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const { busca } = useBusca(query, 'livros');

  const books = useMemo<BookOption[]>(() => {
    if (busca.situacao !== 'sucesso') {
      return [];
    }

    if (busca.dados.tipo !== 'livros') {
      return [];
    }

    return busca.dados.itens.slice(0, 5).map((item) => ({
      id: item.id,
      title: item.titulo,
      author: item.autor ?? 'Autor desconhecido',
      genre: 'Livro',
      imageUrl: item.capa,
    }));
  }, [busca]);

  const isSearching = busca.situacao === 'carregando';

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
          <Text style={styles.libraryLabel}>Sua Estante</Text>
        </View>

        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.listContent}
          keyboardShouldPersistTaps="handled"
        >
          {isSearching && (
            <View style={styles.loadingRow}>
              <ActivityIndicator color={colors.primary} />
              <Text style={styles.loadingText}>Buscando livros...</Text>
            </View>
          )}

          {!query.trim() && !isSearching && (
            <Text style={styles.emptyState}>Digite para buscar um livro.</Text>
          )}

          {!!query.trim() && !isSearching && books.length === 0 && (
            <Text style={styles.emptyState}>Nenhum livro encontrado.</Text>
          )}

          {books.map((book) => (
            <ListItem
              key={book.id}
              variant="book"
              title={book.title}
              subtitle={book.author}
              genre={book.genre}
              imageUrl={book.imageUrl}
              selected={selectedId === book.id}
              onPress={() => setSelectedId(book.id)}
            />
          ))}
        </ScrollView>

        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Confirmar livro"
          onPress={() => {
            /* ação do desafio futura */
          }}
          style={[styles.confirmButton, !selectedId && styles.confirmButtonDisabled]}
          disabled={!selectedId}
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
  libraryLabel: {
    ...textStyles.bodyStrong,
    alignSelf: 'flex-start',
    backgroundColor: '#F1D9DD',
    color: colors.primary,
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 6,
    overflow: 'hidden',
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
