import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { AppHeader } from '@/components/AppHeader';
import { BookCover } from '@/components/BookCover';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { colors, shadows, sizes, spacing, textStyles } from '@/theme';

import type { BibliotecaResponse, LivroBiblioteca } from './api';
import type { BibliotecaState } from './useBiblioteca';

export type MeusLivrosViewProps = BibliotecaState & {
  /** A tela decide o destino; a view só avisa qual livro foi tocado. */
  onLivroPress: (livro: LivroBiblioteca) => void;
};

// Copy da aba lida do frame 2043-609 (#30, "Definições travadas").
const COPY = {
  titulo: 'Meus Livros',
  subtitulo: 'Explore sua biblioteca',
  favoritos: 'Livros favoritos',
  ultimos: 'Últimos livros',
  adicionarLivro: '+ Adicionar livro',
  // Sem copy no Figma: as seções de desejo e lidos e os vazios/erros são divergência assumida.
  desejo: 'Quero ler',
  lidos: 'Livros lidos',
  bibliotecaVazia: 'Você ainda não tem livros na biblioteca.',
  semLendo: 'Nenhum livro em leitura.',
  semLidos: 'Nenhum livro lido ainda.',
  tentarDeNovo: 'Tentar de novo',
  emDesenvolvimento: 'Funcionalidade em desenvolvimento',
} as const;

const AVISO_VISIVEL_MS = 2500;

function SectionTitle({ children }: { children: string }) {
  return <Text className="font-inter-bold text-h2 text-text">{children}</Text>;
}

function Carregando() {
  return (
    <View
      className="items-center justify-center py-8"
      accessibilityLabel="Carregando"
    >
      <ActivityIndicator color={colors.primary} />
    </View>
  );
}

function Erro({
  mensagem,
  onRetry,
}: {
  mensagem: string;
  onRetry: () => void;
}) {
  return (
    <EmptyState
      icon={BookIcon}
      message={mensagem}
      action={
        <Pressable
          accessibilityRole="button"
          onPress={onRetry}
          className="items-center justify-center rounded-pill border border-primary px-6"
          style={{ height: sizes.buttonHeight }}
        >
          <Text style={[textStyles.button, { color: colors.primary }]}>
            {COPY.tentarDeNovo}
          </Text>
        </Pressable>
      }
    />
  );
}

type ListaProps = {
  livros: readonly LivroBiblioteca[];
  onLivroPress: (livro: LivroBiblioteca) => void;
};

function Capa({
  livro,
  onLivroPress,
}: {
  livro: LivroBiblioteca;
  onLivroPress: (livro: LivroBiblioteca) => void;
}) {
  return (
    <BookCover
      source={livro.capaUrl}
      accessibilityLabel={livro.titulo}
      onPress={() => onLivroPress(livro)}
    />
  );
}

/** Fileira horizontal rolável, como a de favoritos no Figma. */
function FileiraDeCapas({
  livros,
  onLivroPress,
  children,
}: ListaProps & { children?: React.ReactNode }) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: spacing[3] }}
    >
      {livros.map((livro) => (
        <Capa key={livro.livroId} livro={livro} onLivroPress={onLivroPress} />
      ))}
      {children}
    </ScrollView>
  );
}

/** Grade de 4 colunas, como a de últimos livros no Figma. */
function GradeDeCapas({ livros, onLivroPress }: ListaProps) {
  return (
    <View className="flex-row flex-wrap gap-3">
      {livros.map((livro) => (
        <Capa key={livro.livroId} livro={livro} onLivroPress={onLivroPress} />
      ))}
    </View>
  );
}

/**
 * Aviso local com a copy do Toast (#29). Substituir pelo `Toast` quando o
 * PR #66 entrar em dev: a tela só precisa trocar `avisar()` por `showToast()`.
 */
function useAvisoEmDesenvolvimento() {
  const [visivel, setVisivel] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (timer.current) clearTimeout(timer.current);
    },
    []
  );

  function avisar() {
    if (timer.current) clearTimeout(timer.current);
    setVisivel(true);
    timer.current = setTimeout(() => {
      setVisivel(false);
      timer.current = null;
    }, AVISO_VISIVEL_MS);
  }

  return { visivel, avisar };
}

function AvisoEmDesenvolvimento({ bottom }: { bottom: number }) {
  return (
    <View
      pointerEvents="none"
      className="absolute left-0 right-0 items-center px-6"
      style={{ bottom }}
      accessibilityLiveRegion="polite"
    >
      <View
        className="rounded-pill bg-primary px-5 py-3"
        style={shadows.floating}
      >
        <Text
          style={[textStyles.bodySmallStrong, { color: colors.textInverse }]}
        >
          {COPY.emDesenvolvimento}
        </Text>
      </View>
    </View>
  );
}

function Secoes({
  dados,
  onLivroPress,
  onAdicionarFavorito,
}: {
  dados: BibliotecaResponse;
  onLivroPress: (livro: LivroBiblioteca) => void;
  onAdicionarFavorito: () => void;
}) {
  const { favoritos, lendo, desejo, lidos } = dados;
  // Favorito é flag de um item com estado, então biblioteca vazia = as três listas vazias.
  const bibliotecaVazia =
    lendo.length === 0 && desejo.length === 0 && lidos.length === 0;

  return (
    <>
      <View className="gap-3">
        <SectionTitle>{COPY.favoritos}</SectionTitle>
        <FileiraDeCapas livros={favoritos} onLivroPress={onLivroPress}>
          <BookCover variant="add-favorite" onPress={onAdicionarFavorito} />
        </FileiraDeCapas>
      </View>

      {bibliotecaVazia ? (
        <EmptyState icon={BookIcon} message={COPY.bibliotecaVazia} />
      ) : (
        <>
          <View className="gap-3">
            <SectionTitle>{COPY.ultimos}</SectionTitle>
            {lendo.length === 0 ? (
              <EmptyState icon={BookIcon} message={COPY.semLendo} />
            ) : (
              <GradeDeCapas livros={lendo} onLivroPress={onLivroPress} />
            )}
          </View>

          {desejo.length > 0 && (
            <View className="gap-3">
              <SectionTitle>{COPY.desejo}</SectionTitle>
              <FileiraDeCapas livros={desejo} onLivroPress={onLivroPress} />
            </View>
          )}

          <View className="gap-3">
            <SectionTitle>{COPY.lidos}</SectionTitle>
            {lidos.length === 0 ? (
              <EmptyState icon={BookIcon} message={COPY.semLidos} />
            ) : (
              <GradeDeCapas livros={lidos} onLivroPress={onLivroPress} />
            )}
          </View>
        </>
      )}
    </>
  );
}

/** Aba Meus Livros. Não busca dados nem navega: recebe tudo de `useBiblioteca` e da tela. */
export function MeusLivrosView({
  biblioteca,
  recarregar,
  onLivroPress,
}: MeusLivrosViewProps) {
  const insets = useSafeAreaInsets();
  const aviso = useAvisoEmDesenvolvimento();

  return (
    <View className="flex-1 bg-surface">
      <AppHeader title={COPY.titulo} subtitle={COPY.subtitulo} />

      <ScrollView
        className="flex-1"
        contentContainerStyle={{
          paddingHorizontal: spacing[4],
          paddingTop: spacing[6],
          paddingBottom: spacing[10],
          gap: spacing[6],
        }}
        showsVerticalScrollIndicator={false}
      >
        {biblioteca.situacao === 'carregando' && (
          <View className="gap-3">
            <SectionTitle>{COPY.favoritos}</SectionTitle>
            <Carregando />
          </View>
        )}
        {biblioteca.situacao === 'erro' && (
          <View className="gap-3">
            <SectionTitle>{COPY.favoritos}</SectionTitle>
            <Erro mensagem={biblioteca.mensagem} onRetry={recarregar} />
          </View>
        )}
        {biblioteca.situacao === 'sucesso' && (
          <Secoes
            dados={biblioteca.dados}
            onLivroPress={onLivroPress}
            onAdicionarFavorito={aviso.avisar}
          />
        )}

        <Pressable
          accessibilityRole="button"
          onPress={aviso.avisar}
          className="items-center justify-center rounded-pill bg-primary"
          style={[{ height: sizes.buttonHeight }, shadows.button]}
        >
          <Text style={[textStyles.button, { color: colors.textInverse }]}>
            {COPY.adicionarLivro}
          </Text>
        </Pressable>
      </ScrollView>

      {aviso.visivel && (
        <AvisoEmDesenvolvimento bottom={insets.bottom + spacing[6]} />
      )}
    </View>
  );
}

export default MeusLivrosView;
