import { useState } from 'react';
import { useRouter } from 'expo-router';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { AppHeader } from '@/components/AppHeader';
import { BookCover } from '@/components/BookCover';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { ListItem } from '@/components/ListItem';
import { PrimaryButton } from '@/components/PrimaryButton';
import { SearchInput } from '@/components/SearchInput';
import { colors, spacing, textStyles } from '@/theme';

import type { LivroAdicionado, ResultadoCatalogo } from './api';
import { SelecionarListaSheet } from './SelecionarListaSheet';
import { useBuscaCatalogo } from './useBuscaCatalogo';

const COPY = {
  titulo: 'Adicionar Livro',
  placeholder: 'Busque por título, autor ou ISBN',
  semResultado: 'Nenhum livro encontrado.',
  tentarDeNovo: 'Tentar de novo',
  adicionarNovo: 'Adicionar novo livro',
  fechar: 'Fechar',
  sucesso: 'Livro adicionado com sucesso!',
} as const;

/** Busca no catálogo, modal de lista e confirmação (#155). */
export function AdicionarLivroScreen() {
  const router = useRouter();
  const [termo, setTermo] = useState('');
  const [adicionouLivro, setAdicionouLivro] = useState(false);
  const { busca, recarregar } = useBuscaCatalogo(termo);
  const [livroSelecionado, setLivroSelecionado] =
    useState<ResultadoCatalogo | null>(null);
  const [confirmacao, setConfirmacao] = useState<LivroAdicionado | null>(null);

  function voltar() {
    if (adicionouLivro) {
      fecharEVoltarParaBiblioteca();
      return;
    }
    if (router.canGoBack()) router.back();
    else router.replace('/meus-livros');
  }

  function fecharEVoltarParaBiblioteca() {
    router.dismissTo({
      pathname: '/meus-livros',
      params: { livroAdicionado: String(Date.now()) },
    });
  }

  function recomecarBusca() {
    setConfirmacao(null);
    setTermo('');
  }

  if (confirmacao) {
    return (
      <View style={styles.tela}>
        <AppHeader
          compact
          title={COPY.titulo}
          showBack
          onBackPress={fecharEVoltarParaBiblioteca}
        />
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={COPY.fechar}
          onPress={fecharEVoltarParaBiblioteca}
          hitSlop={spacing[3]}
          style={styles.fechar}
        >
          <Text style={styles.fecharTexto}>✕</Text>
        </Pressable>
        <View style={styles.confirmacao}>
          <BookCover
            source={confirmacao.capaUrl}
            size="detail"
            accessibilityLabel={`Capa de ${confirmacao.titulo}`}
          />
          <Text style={styles.mensagemSucesso} accessibilityLiveRegion="polite">
            {COPY.sucesso}
          </Text>
          <View style={styles.acoesConfirmacao}>
            <PrimaryButton
              label={COPY.adicionarNovo}
              variant="outline"
              onPress={recomecarBusca}
            />
            <PrimaryButton
              label={COPY.fechar}
              onPress={fecharEVoltarParaBiblioteca}
            />
          </View>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.tela}>
      <AppHeader compact title={COPY.titulo} showBack onBackPress={voltar} />

      <View style={styles.buscaContainer}>
        <SearchInput
          accessibilityLabel={COPY.placeholder}
          placeholder={COPY.placeholder}
          value={termo}
          onChangeText={setTermo}
          variant="outlined"
          returnKeyType="search"
        />
      </View>

      {busca.situacao === 'carregando' && (
        <View style={styles.estado}>
          <ActivityIndicator
            color={colors.primary}
            accessibilityLabel="Carregando"
          />
        </View>
      )}

      {busca.situacao === 'erro' && (
        <EmptyState
          icon={BookIcon}
          message={busca.mensagem}
          action={
            <PrimaryButton
              label={COPY.tentarDeNovo}
              variant="outline"
              onPress={recarregar}
            />
          }
        />
      )}

      {busca.situacao === 'sucesso' && busca.resultados.length === 0 && (
        <EmptyState icon={SearchIcon} message={COPY.semResultado} />
      )}

      {busca.situacao === 'sucesso' && busca.resultados.length > 0 && (
        <ScrollView
          style={styles.lista}
          contentContainerStyle={styles.conteudoLista}
          keyboardDismissMode="on-drag"
          keyboardShouldPersistTaps="handled"
        >
          <Text style={styles.resultadosTitulo}>Resultados</Text>
          {busca.resultados.map((item) => (
            <ListItem
              key={item.volumeId}
              variant="book"
              title={item.titulo}
              subtitle={item.autor ?? undefined}
              imageUrl={item.capaUrl}
              onPress={() => setLivroSelecionado(item)}
            />
          ))}
        </ScrollView>
      )}

      <SelecionarListaSheet
        key={livroSelecionado?.volumeId ?? 'fechado'}
        visible={livroSelecionado !== null}
        livro={livroSelecionado}
        onClose={() => setLivroSelecionado(null)}
        onSuccess={(resultado) => {
          setLivroSelecionado(null);
          setAdicionouLivro(true);
          setConfirmacao(resultado);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  fechar: { alignSelf: 'flex-end', padding: spacing[4] },
  fecharTexto: { ...textStyles.h1, color: colors.text },
  resultadosTitulo: { ...textStyles.h3, color: colors.text },
  tela: { flex: 1, backgroundColor: colors.surfaceMuted },
  buscaContainer: {
    paddingHorizontal: spacing[6],
    paddingTop: spacing[6],
    paddingBottom: spacing[4],
  },
  estado: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  lista: { flex: 1 },
  conteudoLista: {
    paddingHorizontal: spacing[6],
    paddingBottom: spacing[4],
    gap: spacing[4],
  },
  confirmacao: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing[6],
    paddingHorizontal: spacing[6],
  },
  mensagemSucesso: {
    ...textStyles.h3,
    color: colors.text,
    textAlign: 'center',
  },
  acoesConfirmacao: { alignSelf: 'stretch', gap: spacing[2] },
});

export default AdicionarLivroScreen;
