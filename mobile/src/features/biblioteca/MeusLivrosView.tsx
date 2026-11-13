import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';

import { AppHeader } from '@/components/AppHeader';
import { BookCover } from '@/components/BookCover';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ProgressBar } from '@/components/ProgressBar';
import { colors, spacing } from '@/theme';
import type { ListaBiblioteca, LivroBiblioteca } from './api';
import type { BibliotecaState } from './useBiblioteca';

export type MeusLivrosViewProps = BibliotecaState & {
  onLivroPress: (livro: LivroBiblioteca) => void;
  onAdicionarLivro: () => void;
  onRegistrarLeitura: (livro: LivroBiblioteca) => void;
};

const FILTROS: { lista: ListaBiblioteca; titulo: string; vazio: string }[] = [
  {
    lista: 'lendo',
    titulo: 'Lendo',
    vazio: 'Escolha sua próxima leitura entre os livros que você quer ler.',
  },
  {
    lista: 'desejo',
    titulo: 'Quero ler',
    vazio: 'Salve aqui os livros que você quer ler depois.',
  },
  {
    lista: 'lidos',
    titulo: 'Lidos',
    vazio: 'Seus livros concluídos vão aparecer aqui.',
  },
  {
    lista: 'favoritos',
    titulo: 'Favoritos',
    vazio: 'Abra um livro e use “Minhas listas” para marcar um favorito.',
  },
];

export function MeusLivrosView({
  biblioteca,
  recarregar,
  carregarMais,
  onLivroPress,
  onAdicionarLivro,
  onRegistrarLeitura,
}: MeusLivrosViewProps) {
  const [selecionada, setSelecionada] = useState<ListaBiblioteca>('lendo');
  const dados = biblioteca.situacao === 'sucesso' ? biblioteca.dados : null;
  const vazia =
    dados &&
    ['lendo', 'desejo', 'lidos'].every(
      (lista) => dados[lista as ListaBiblioteca].itens.length === 0
    );
  const atual = dados?.lendo.itens[0];
  const lista = dados?.[selecionada];
  const itens =
    lista?.itens.filter(
      (livro) => selecionada !== 'lendo' || livro.livroId !== atual?.livroId
    ) ?? [];

  return (
    <View className="flex-1 bg-surface">
      <AppHeader
        compact
        title="Meus Livros"
        subtitle="Sua próxima página começa aqui"
      />
      {!vazia && (
        <View className="px-4 pt-4">
          <PrimaryButton
            label="+ Adicionar livro"
            onPress={onAdicionarLivro}
            variant="outline"
          />
        </View>
      )}
      <ScrollView
        className="flex-1"
        contentContainerStyle={{
          padding: spacing[4],
          paddingBottom: spacing[10],
          gap: spacing[6],
          flexGrow: 1,
        }}
      >
        {biblioteca.situacao === 'carregando' && (
          <ActivityIndicator
            color={colors.primary}
            accessibilityLabel="Carregando biblioteca"
          />
        )}
        {biblioteca.situacao === 'erro' && (
          <EmptyState
            icon={BookIcon}
            message={biblioteca.mensagem}
            action={
              <PrimaryButton
                label="Tentar de novo"
                onPress={recarregar}
                variant="outline"
              />
            }
          />
        )}
        {vazia ? (
          <EmptyState
            icon={BookIcon}
            message="Sua próxima leitura começa aqui. Adicione um livro para organizar suas leituras e acompanhar seu progresso."
            action={
              <PrimaryButton
                label="Adicionar primeiro livro"
                onPress={onAdicionarLivro}
              />
            }
          />
        ) : dados ? (
          <>
            {atual && (
              <View className="gap-3">
                <Text className="font-inter-bold text-h2 text-text">
                  Continuar lendo
                </Text>
                <View className="gap-4 rounded-lg bg-surface-muted p-4">
                  <View className="flex-row gap-3">
                    <BookCover
                      source={atual.capaUrl}
                      accessibilityLabel={atual.titulo}
                      onPress={() => onLivroPress(atual)}
                    />
                    <View className="min-w-0 flex-1 justify-center gap-2">
                      <Text
                        className="font-inter-bold text-h3 text-text"
                        numberOfLines={3}
                      >
                        {atual.titulo}
                      </Text>
                      {atual.autor && (
                        <Text
                          className="font-inter text-bodySmall text-text-secondary"
                          numberOfLines={2}
                        >
                          {atual.autor}
                        </Text>
                      )}
                      <Text className="font-inter text-caption text-text-secondary">
                        {atual.paginaAtual} de {atual.totalPaginas} páginas
                      </Text>
                    </View>
                  </View>
                  <ProgressBar
                    progress={
                      atual.totalPaginas > 0
                        ? (atual.paginaAtual / atual.totalPaginas) * 100
                        : 0
                    }
                    label="Seu progresso"
                  />
                  <PrimaryButton
                    label="Registrar leitura"
                    onPress={() => onRegistrarLeitura(atual)}
                  />
                </View>
              </View>
            )}
            <View className="gap-3">
              <Text className="font-inter-bold text-h2 text-text">
                Sua biblioteca
              </Text>
              <View className="flex-row gap-2">
                {FILTROS.map(({ lista: chave, titulo }) => (
                  <Pressable
                    key={chave}
                    accessibilityRole="tab"
                    accessibilityLabel={titulo}
                    accessibilityState={{ selected: selecionada === chave }}
                    onPress={() => setSelecionada(chave)}
                    className={`min-h-12 min-w-0 flex-1 items-center justify-center rounded-pill border px-1 py-2 ${selecionada === chave ? 'border-primary bg-primary' : 'border-border-strong bg-surface'}`}
                  >
                    <Text
                      className={`font-inter-semibold text-caption ${selecionada === chave ? 'text-text-inverse' : 'text-text'}`}
                    >
                      {titulo}
                    </Text>
                  </Pressable>
                ))}
              </View>
              {itens.map((livro) => (
                <Pressable
                  key={livro.livroId}
                  accessibilityRole="button"
                  accessibilityLabel={livro.titulo}
                  onPress={() => onLivroPress(livro)}
                  className="flex-row items-center gap-4 rounded-lg bg-surface-muted p-3"
                >
                  <BookCover
                    source={livro.capaUrl}
                    accessibilityLabel={`Capa de ${livro.titulo}`}
                  />
                  <View className="min-w-0 flex-1 gap-2">
                    <Text
                      className="font-inter-bold text-body text-text"
                      numberOfLines={3}
                    >
                      {livro.titulo}
                    </Text>
                    {livro.autor && (
                      <Text
                        className="font-inter text-bodySmall text-text-secondary"
                        numberOfLines={2}
                      >
                        {livro.autor}
                      </Text>
                    )}
                    {selecionada === 'lendo' && (
                      <Text className="font-inter text-caption text-text-secondary">
                        {livro.paginaAtual} de {livro.totalPaginas} páginas
                      </Text>
                    )}
                  </View>
                </Pressable>
              ))}
              {lista && lista.itens.length === 0 && (
                <EmptyState
                  icon={BookIcon}
                  message={
                    FILTROS.find((filtro) => filtro.lista === selecionada)!
                      .vazio
                  }
                />
              )}
              {selecionada === 'lendo' && atual && itens.length === 0 && (
                <Text className="font-inter text-bodySmall text-text-secondary">
                  Sua leitura em andamento está em destaque acima.
                </Text>
              )}
              {lista?.proximoCursor && (
                <PrimaryButton
                  label={
                    lista.carregandoMais
                      ? 'Carregando livros'
                      : lista.erroAoCarregarMais
                        ? 'Tentar de novo'
                        : 'Ver mais'
                  }
                  variant="outline"
                  loading={lista.carregandoMais}
                  onPress={() => carregarMais(selecionada)}
                />
              )}
            </View>
          </>
        ) : null}
      </ScrollView>
    </View>
  );
}

export default MeusLivrosView;
