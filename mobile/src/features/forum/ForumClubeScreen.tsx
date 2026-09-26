import { useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useRouter, type Href } from 'expo-router';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PostCard } from '@/components/PostCard';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { tempoRelativo } from '@/features/livro/model';
import { colors, spacing, typography } from '@/theme';

import { useForumClube } from './useForumClube';

const COPY = {
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar para o clube',
  // O Figma nao desenha o forum vazio e nao ha copy aprovada; o texto abaixo foi definido com a
  // gestao. Com o seed de #13 o estado nao aparece em uso normal: existe como defesa.
  semPosts: 'Ainda não há posts sobre a leitura atual deste clube.',
} as const;

function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

export function ForumClubeScreen({ clubeId }: { clubeId: string }) {
  const router = useRouter();
  const { forum, recarregar, alternarCurtida } = useForumClube(clubeId);
  const { showErrorToast } = useToastContext();
  const [curtidasPendentes, setCurtidasPendentes] = useState<Set<string>>(
    new Set()
  );

  const dados = forum.situacao === 'sucesso' ? forum.dados : null;

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

  function voltar() {
    if (router.canGoBack()) {
      router.back();
      return;
    }
    router.replace(rotaDoClube(clubeId));
  }

  if (forum.situacao === 'erro') {
    return (
      <View style={styles.tela}>
        <EmptyState
          icon={BookIcon}
          message={forum.mensagem}
          action={
            <View style={styles.acoesDoErro}>
              <PrimaryButton
                label={COPY.tentarDeNovo}
                onPress={recarregar}
                variant="outline"
              />
              <PrimaryButton
                label={COPY.voltar}
                onPress={voltar}
                variant="outline"
              />
            </View>
          }
        />
      </View>
    );
  }

  const livro = dados?.clube.livroAtual;
  // Título do livro em negrito seguido do autor em regular, como a definição travada pede.
  const subtitulo = livro ? (
    <>
      <Text style={styles.tituloDoLivro}>{livro.titulo}</Text>
      {livro.autor ? ` - ${livro.autor}` : ''}
    </>
  ) : undefined;

  return (
    <View style={styles.tela}>
      <ScrollView contentContainerStyle={styles.rolagem}>
        <AppHeader
          titleAlign="center"
          showBack
          onBackPress={voltar}
          title={dados?.clube.nome ?? ''}
          subtitle={subtitulo}
          coverUrl={livro?.capaUrl ?? undefined}
          titleOverflow="wrap"
          titleNumberOfLines={2}
        />

        <View style={styles.conteudo}>
          {dados === null ? (
            <View
              style={styles.carregando}
              accessibilityLabel="Carregando posts"
            >
              <ActivityIndicator color={colors.primary} />
            </View>
          ) : dados.posts.length === 0 ? (
            <EmptyState icon={BookIcon} message={COPY.semPosts} />
          ) : (
            dados.posts.map((post) => (
              <PostCard
                key={post.id}
                authorName={post.autor.nome ?? ''}
                authorPhotoUrl={post.autor.avatarUrl}
                authorStreak={post.autor.sequenciaDias}
                timeAgo={tempoRelativo(post.publicadoEm)}
                text={post.texto}
                likes={post.totalCurtidas}
                likedByMe={post.curtidoPorMim}
                likeDisabled={curtidasPendentes.has(post.id)}
                onLikePress={() => curtirPost(post.id)}
              />
            ))
          )}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.surface },
  tituloDoLivro: { fontFamily: typography.fontFamily.bold },
  rolagem: { flexGrow: 1, paddingBottom: spacing[10] },
  conteudo: { padding: spacing[6], gap: spacing[4], flexGrow: 1 },
  carregando: { flexGrow: 1, justifyContent: 'center', alignItems: 'center' },
  acoesDoErro: { alignSelf: 'stretch', gap: spacing[2] },
});

export default ForumClubeScreen;
