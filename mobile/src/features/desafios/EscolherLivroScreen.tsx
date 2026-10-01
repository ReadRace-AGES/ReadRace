import { useState } from 'react';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { ActivityIndicator, ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { ListItem } from '@/components/ListItem';
import { PrimaryButton } from '@/components/PrimaryButton';
import { SearchInput } from '@/components/SearchInput';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import type { LivroBiblioteca } from '@/features/biblioteca/api';
import { colors, spacing } from '@/theme';

import { useEstante } from './useEscolherLivro';

const ABA_ESTANTE = 'Sua Estante';
const ABA_LENDO = 'Lendo Agora';

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLocaleLowerCase('pt-BR');
}

function casaComTermo(livro: LivroBiblioteca, termo: string) {
  return (
    normalizar(livro.titulo).includes(termo) ||
    normalizar(livro.autor ?? '').includes(termo)
  );
}

export function EscolherLivroScreen() {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { livroId } = useLocalSearchParams<{ livroId?: string }>();
  const [termo, setTermo] = useState('');
  const [aba, setAba] = useState(ABA_ESTANTE);
  const [selecionadoId, setSelecionadoId] = useState(livroId || null);
  const { estado, recarregar } = useEstante();

  const livrosDaAba =
    estado.situacao !== 'sucesso'
      ? []
      : aba === ABA_LENDO
        ? estado.estante.lendo
        : estado.estante.todos;
  const termoNormalizado = normalizar(termo.trim());
  const livros = termoNormalizado
    ? livrosDaAba.filter((livro) => casaComTermo(livro, termoNormalizado))
    : livrosDaAba;
  const livroSelecionado =
    livros.find((livro) => livro.livroId === selecionadoId) ?? null;

  function voltar() {
    if (router.canGoBack()) {
      router.back();
    } else {
      router.dismissTo('/desafiar-amigo');
    }
  }

  function confirmarLivro() {
    if (!livroSelecionado) return;

    router.dismissTo({
      pathname: '/desafiar-amigo',
      params: { livroSelecionado: JSON.stringify(livroSelecionado) },
    });
  }

  function mensagemVazia() {
    if (termoNormalizado) return 'Nenhum livro encontrado.';
    return aba === ABA_LENDO
      ? 'Nenhum livro em leitura.'
      : 'Você ainda não tem livros na biblioteca.';
  }

  return (
    <View style={styles.tela}>
      <AppHeader
        compact
        title="Escolher Livro"
        titleAlign="center"
        showBack
        onBackPress={voltar}
      />

      <View style={styles.filtros}>
        <SearchInput
          accessibilityLabel="Buscar livro por título ou autor"
          placeholder="Buscar título ou autor..."
          value={termo}
          onChangeText={setTermo}
          variant="outlined"
          returnKeyType="search"
        />
        <SegmentedTabs
          options={[ABA_ESTANTE, ABA_LENDO]}
          initialOption={ABA_ESTANTE}
          onChange={setAba}
        />
      </View>

      {estado.situacao === 'carregando' && (
        <View style={styles.estado}>
          <ActivityIndicator
            color={colors.primary}
            accessibilityLabel="Carregando"
          />
        </View>
      )}

      {estado.situacao === 'erro' && (
        <EmptyState
          icon={BookIcon}
          message={estado.mensagem}
          action={
            <PrimaryButton
              label="Tentar de novo"
              variant="outline"
              onPress={recarregar}
            />
          }
        />
      )}

      {estado.situacao === 'sucesso' && livros.length === 0 && (
        <EmptyState
          icon={termoNormalizado ? SearchIcon : BookIcon}
          message={mensagemVazia()}
        />
      )}

      {estado.situacao === 'sucesso' && livros.length > 0 && (
        <ScrollView
          style={styles.lista}
          contentContainerStyle={styles.conteudoLista}
          keyboardDismissMode="on-drag"
          keyboardShouldPersistTaps="handled"
        >
          {livros.map((livro) => (
            <ListItem
              key={livro.livroId}
              variant="book"
              title={livro.titulo}
              subtitle={livro.autor ?? undefined}
              genre={livro.genero ?? undefined}
              imageUrl={livro.capaUrl}
              selected={livro.livroId === selecionadoId}
              onPress={() => setSelecionadoId(livro.livroId)}
            />
          ))}
        </ScrollView>
      )}

      <View
        style={[styles.rodape, { paddingBottom: insets.bottom + spacing[4] }]}
      >
        <PrimaryButton
          label="Confirmar Livro →"
          disabled={!livroSelecionado}
          onPress={confirmarLivro}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surfaceMuted },
  filtros: {
    paddingHorizontal: spacing[6],
    paddingTop: spacing[6],
    paddingBottom: spacing[4],
    gap: spacing[4],
  },
  estado: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  lista: { flex: 1 },
  conteudoLista: {
    paddingHorizontal: spacing[6],
    paddingBottom: spacing[4],
    gap: spacing[4],
  },
  rodape: { paddingHorizontal: spacing[6], paddingTop: spacing[2] },
});

export default EscolherLivroScreen;
