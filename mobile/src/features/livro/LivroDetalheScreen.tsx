import { useCallback, useState } from 'react';
import { useFocusEffect, useRouter } from 'expo-router';
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
import { PostCard } from '@/components/PostCard';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ProgressBar } from '@/components/ProgressBar';
import { useToastContext } from '@/components/toast-provider';
import { RegistrarProgressoSheet } from '@/features/progresso/RegistrarProgressoSheet';
import { bookCover, colors, radius, sizes, spacing, textStyles } from '@/theme';
import { tempoRelativo } from './model';
import { useLivroDetalhe } from './useLivroDetalhe';

export function LivroDetalheScreen({ livroId }: { livroId: string }) {
  const router = useRouter();
  const { estado, recarregar, atualizarProgresso, alternarCurtida } =
    useLivroDetalhe(livroId);
  const { showErrorToast } = useToastContext();
  const [curtidasPendentes, setCurtidasPendentes] = useState<Set<string>>(
    new Set()
  );
  const [modalAberto, setModalAberto] = useState(false);
  useFocusEffect(
    useCallback(
      () => () => {
        setModalAberto(false);
      },
      []
    )
  );
  const voltar = () =>
    router.canGoBack() ? router.back() : router.replace('/meus-livros');
  const detalhe = estado.situacao === 'sucesso' ? estado.dados : null;
  const progresso = detalhe?.progresso;

  async function curtirPost(postId: string) {
    if (curtidasPendentes.has(postId)) return;
    setCurtidasPendentes((atual) => new Set(atual).add(postId));
    try {
      await alternarCurtida(postId);
    } catch (erro) {
      showErrorToast(
        erro instanceof Error
          ? erro.message
          : 'Não foi possível registrar sua curtida. Tente novamente.'
      );
    } finally {
      setCurtidasPendentes((atual) => {
        const proximo = new Set(atual);
        proximo.delete(postId);
        return proximo;
      });
    }
  }

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <AppHeader
          compact
          title={detalhe?.livro.titulo ?? 'Detalhe do livro'}
          subtitle={detalhe?.livro.autor ?? undefined}
          showBack
          onBackPress={voltar}
          titleOverflow="expand"
        />
        {estado.situacao === 'carregando' && (
          <View style={styles.state}>
            <ActivityIndicator
              color={colors.primary}
              accessibilityLabel="Carregando livro"
            />
          </View>
        )}
        {estado.situacao === 'erro' && (
          <View style={styles.state}>
            <Text accessibilityRole="alert" style={styles.text}>
              {estado.mensagem}
            </Text>
            <PrimaryButton label="Tentar de novo" onPress={recarregar} />
            <PrimaryButton label="Voltar" variant="outline" onPress={voltar} />
          </View>
        )}
        {detalhe && (
          <View style={styles.content}>
            <View style={styles.overview}>
              <BookCover
                source={detalhe.livro.capaUrl}
                size="detail"
                accessibilityLabel={`Capa de ${detalhe.livro.titulo}`}
              />
              <View style={styles.details}>
                <View>
                  {detalhe.livro.genero && (
                    <Text style={styles.genre}>{detalhe.livro.genero}</Text>
                  )}
                  <View style={styles.row}>
                    <View style={styles.pages}>
                      <BookIcon />
                      <Text style={styles.pageCount}>
                        {detalhe.livro.totalPaginas} páginas
                      </Text>
                    </View>
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Registrar progresso de leitura"
                      onPress={() => setModalAberto(true)}
                      style={styles.add}
                    >
                      <Text style={styles.plus}>+</Text>
                    </Pressable>
                  </View>
                </View>
                <Pressable
                  accessibilityRole="button"
                  style={({ pressed }) => [
                    styles.progress,
                    pressed && styles.pressed,
                  ]}
                  onPress={() => setModalAberto(true)}
                  accessibilityLabel="Seu progresso. Registrar progresso de leitura"
                >
                  <ProgressBar
                    label="Seu progresso"
                    progress={progresso?.percentual ?? 0}
                  />
                  <Text style={styles.pageDetail}>
                    {progresso?.paginaAtual ?? 0} de{' '}
                    {detalhe.livro.totalPaginas} páginas
                  </Text>
                </Pressable>
              </View>
            </View>
            <Text style={styles.heading}>Posts sobre este livro</Text>
            {detalhe.posts.length === 0 ? (
              <EmptyState
                icon={BookIcon}
                message="Nenhum post sobre este livro."
              />
            ) : (
              detalhe.posts.map((post) => (
                <PostCard
                  key={post.id}
                  authorName={post.autor.nome}
                  authorPhotoUrl={post.autor.avatarUrl}
                  authorStreak={
                    post.autor.sequenciaDias == null
                      ? null
                      : `${post.autor.sequenciaDias} dias`
                  }
                  timeAgo={tempoRelativo(post.criadoEm)}
                  text={post.texto}
                  likes={post.curtidas}
                  likedByMe={post.curtidoPorMim}
                  likeDisabled={curtidasPendentes.has(post.id)}
                  onLikePress={() => curtirPost(post.id)}
                />
              ))
            )}
          </View>
        )}
      </ScrollView>
      {detalhe && (
        <RegistrarProgressoSheet
          visible={modalAberto}
          livroId={detalhe.livro.id}
          totalPaginas={detalhe.livro.totalPaginas}
          paginaAtual={progresso?.paginaAtual ?? 0}
          paginaMaximaAlcancada={progresso?.paginaMaximaAlcancada ?? 0}
          onClose={() => setModalAberto(false)}
          onSuccess={atualizarProgresso}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  scroll: { flexGrow: 1, paddingBottom: spacing[6] },
  content: { padding: spacing[6], gap: spacing[4] },
  state: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing[6],
    gap: spacing[4],
  },
  overview: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing[3] },
  details: {
    flex: 1,
    minWidth: 0,
    minHeight: bookCover.detail.height,
    justifyContent: 'space-between',
    gap: spacing[2],
  },
  progress: { paddingVertical: spacing[1] },
  pressed: { opacity: 0.8 },
  pageCount: {
    ...textStyles.caption,
    color: colors.textSecondary,
    flexShrink: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  pages: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[2],
    flex: 1,
    minWidth: 0,
  },
  text: { ...textStyles.body, color: colors.text },
  heading: { ...textStyles.h3, color: colors.text },
  genre: {
    ...textStyles.captionStrong,
    color: colors.primary,
    backgroundColor: colors.surfacePink,
    borderRadius: radius.pill,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[1],
    alignSelf: 'flex-start',
  },
  add: {
    minWidth: sizes.buttonHeight,
    minHeight: sizes.buttonHeight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  plus: { ...textStyles.h1, color: colors.primary },
  pageDetail: {
    ...textStyles.bodySmall,
    color: colors.textSecondary,
    marginTop: spacing[2],
  },
});
