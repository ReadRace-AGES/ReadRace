import { useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
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
import {
  bookCover,
  colors,
  shadows,
  sizes,
  spacing,
  textStyles,
} from '@/theme';

import type { ListaBiblioteca, LivroBiblioteca } from './api';
import type {
  Biblioteca,
  BibliotecaState,
  ListaCarregada,
} from './useBiblioteca';

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
  // Sem copy no Figma: as seções de desejo e lidos, o "ver mais" e os vazios/erros são
  // divergência assumida.
  desejo: 'Quero ler',
  lidos: 'Livros lidos',
  verMais: 'Ver mais',
  bibliotecaVazia: 'Você ainda não tem livros na biblioteca.',
  semLendo: 'Nenhum livro em leitura.',
  semLidos: 'Nenhum livro lido ainda.',
  tentarDeNovo: 'Tentar de novo',
  emDesenvolvimento: 'Funcionalidade em desenvolvimento',
} as const;

const AVISO_VISIVEL_MS = 2500;
// A lista do fim da página carrega sozinha quando faltam menos que isto para o fim do scroll.
const MARGEM_SCROLL_INFINITO = bookCover.grid.height * 2;

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

function BotaoSecundario({
  label,
  onPress,
}: {
  label: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      className="items-center justify-center rounded-pill border border-primary px-6"
      style={{ height: sizes.buttonHeight }}
    >
      <Text style={[textStyles.button, { color: colors.primary }]}>
        {label}
      </Text>
    </Pressable>
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
      action={<BotaoSecundario label={COPY.tentarDeNovo} onPress={onRetry} />}
    />
  );
}

type ListaProps = {
  lista: ListaCarregada;
  onLivroPress: (livro: LivroBiblioteca) => void;
  onVerMais: () => void;
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

/** Cartão do tamanho de uma capa, para pedir a próxima página numa fileira. */
function CartaoVerMais({
  lista,
  onVerMais,
}: Pick<ListaProps, 'lista' | 'onVerMais'>) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={COPY.verMais}
      onPress={onVerMais}
      disabled={lista.carregandoMais}
      className="items-center justify-center rounded-md bg-surface-muted px-2"
      style={bookCover.grid}
    >
      {lista.carregandoMais ? (
        <ActivityIndicator color={colors.primary} />
      ) : (
        <Text
          className="text-center font-inter-bold text-bodySmall text-primary"
          numberOfLines={2}
        >
          {lista.erroAoCarregarMais ? COPY.tentarDeNovo : COPY.verMais}
        </Text>
      )}
    </Pressable>
  );
}

/** Rodapé de uma grade: pede a próxima página ou mostra que ela está vindo. */
function RodapeVerMais({
  lista,
  onVerMais,
}: Pick<ListaProps, 'lista' | 'onVerMais'>) {
  if (!lista.proximoCursor) return null;
  if (lista.carregandoMais) return <Carregando />;

  return (
    <View className="items-center">
      <BotaoSecundario
        label={lista.erroAoCarregarMais ? COPY.tentarDeNovo : COPY.verMais}
        onPress={onVerMais}
      />
    </View>
  );
}

/** Fileira horizontal rolável, como a de favoritos no Figma. */
function FileiraDeCapas({
  lista,
  onLivroPress,
  onVerMais,
  children,
}: ListaProps & { children?: React.ReactNode }) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerStyle={{ gap: spacing[3] }}
    >
      {lista.itens.map((livro) => (
        <Capa key={livro.livroId} livro={livro} onLivroPress={onLivroPress} />
      ))}
      {lista.proximoCursor && (
        <CartaoVerMais lista={lista} onVerMais={onVerMais} />
      )}
      {children}
    </ScrollView>
  );
}

/** Grade de 4 colunas, como a de últimos livros no Figma. */
function GradeDeCapas({ lista, onLivroPress, onVerMais }: ListaProps) {
  return (
    <View className="gap-3">
      <View className="flex-row flex-wrap gap-3">
        {lista.itens.map((livro) => (
          <Capa key={livro.livroId} livro={livro} onLivroPress={onLivroPress} />
        ))}
      </View>
      <RodapeVerMais lista={lista} onVerMais={onVerMais} />
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
  onVerMais,
  onAdicionarFavorito,
}: {
  dados: Biblioteca;
  onLivroPress: (livro: LivroBiblioteca) => void;
  onVerMais: (lista: ListaBiblioteca) => void;
  onAdicionarFavorito: () => void;
}) {
  const { favoritos, lendo, desejo, lidos } = dados;
  // Favorito é flag de um item com estado, então biblioteca vazia = as três listas vazias.
  const bibliotecaVazia =
    lendo.itens.length === 0 &&
    desejo.itens.length === 0 &&
    lidos.itens.length === 0;

  return (
    <>
      <View className="gap-3">
        <SectionTitle>{COPY.favoritos}</SectionTitle>
        <FileiraDeCapas
          lista={favoritos}
          onLivroPress={onLivroPress}
          onVerMais={() => onVerMais('favoritos')}
        >
          <BookCover variant="add-favorite" onPress={onAdicionarFavorito} />
        </FileiraDeCapas>
      </View>

      {bibliotecaVazia ? (
        <EmptyState icon={BookIcon} message={COPY.bibliotecaVazia} />
      ) : (
        <>
          <View className="gap-3">
            <SectionTitle>{COPY.ultimos}</SectionTitle>
            {lendo.itens.length === 0 ? (
              <EmptyState icon={BookIcon} message={COPY.semLendo} />
            ) : (
              <GradeDeCapas
                lista={lendo}
                onLivroPress={onLivroPress}
                onVerMais={() => onVerMais('lendo')}
              />
            )}
          </View>

          {desejo.itens.length > 0 && (
            <View className="gap-3">
              <SectionTitle>{COPY.desejo}</SectionTitle>
              <FileiraDeCapas
                lista={desejo}
                onLivroPress={onLivroPress}
                onVerMais={() => onVerMais('desejo')}
              />
            </View>
          )}

          <View className="gap-3">
            <SectionTitle>{COPY.lidos}</SectionTitle>
            {lidos.itens.length === 0 ? (
              <EmptyState icon={BookIcon} message={COPY.semLidos} />
            ) : (
              <GradeDeCapas
                lista={lidos}
                onLivroPress={onLivroPress}
                onVerMais={() => onVerMais('lidos')}
              />
            )}
          </View>
        </>
      )}
    </>
  );
}

function pertoDoFim({
  layoutMeasurement,
  contentOffset,
  contentSize,
}: NativeScrollEvent) {
  return (
    layoutMeasurement.height + contentOffset.y >=
    contentSize.height - MARGEM_SCROLL_INFINITO
  );
}

/** Aba Meus Livros. Não busca dados nem navega: recebe tudo de `useBiblioteca` e da tela. */
export function MeusLivrosView({
  biblioteca,
  recarregar,
  carregarMais,
  onLivroPress,
}: MeusLivrosViewProps) {
  const insets = useSafeAreaInsets();
  const aviso = useAvisoEmDesenvolvimento();

  // "Livros lidos" é a lista que cresce sem limite e fica no fim da página, então ela
  // carrega sozinha no scroll. As outras pedem página com "Ver mais", senão duas listas
  // disputariam o mesmo fim de scroll. Se a primeira página não enche a tela, o scroll
  // nunca dispara: por isso o rodapé "Ver mais" continua visível também nos lidos.
  function aoRolar(evento: NativeSyntheticEvent<NativeScrollEvent>) {
    if (biblioteca.situacao !== 'sucesso') return;
    const lidos = biblioteca.dados.lidos;
    if (lidos.erroAoCarregarMais) return;
    if (pertoDoFim(evento.nativeEvent)) carregarMais('lidos');
  }

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
        onScroll={aoRolar}
        scrollEventThrottle={100}
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
            onVerMais={carregarMais}
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
