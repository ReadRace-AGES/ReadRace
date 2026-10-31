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
import { ComentariosSheet } from '@/features/comentarios/ComentariosSheet';
import { tempoRelativo } from '@/features/livro/model';
import { colors, spacing, typography } from '@/theme';

import { useForumClube } from './useForumClube';
import { NovoPostSheet } from './NovoPostSheet';

const COPY = {
  tentarDeNovo: 'Tentar de novo',
  voltar: 'Voltar para o clube',
  semPosts: 'Ainda não há posts sobre a leitura atual deste clube.',
} as const;

function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

export function ForumClubeScreen({ clubeId }: { clubeId: string }) {
  return <ForumDoClube key={clubeId} clubeId={clubeId} />;
}

function ForumDoClube({ clubeId }: { clubeId: string }) {
  const router = useRouter();

  const {
    forum,
    recarregar,
    alternarCurtida,
    curtidasPendentes,
    adicionarPost,
    registrarComentario,
  } = useForumClube(clubeId);

  const { showErrorToast } = useToastContext();

  const [novoPostAberto, setNovoPostAberto] = useState(false);
  const [postComentariosId, setPostComentariosId] = useState<string | null>(
    null
  );

  const dados = forum.situacao === 'sucesso' ? forum.dados : null;

  async function curtirPost(postId: string) {
    try {
      await alternarCurtida(postId);
    } catch (erro) {
      showErrorToast(
        erro instanceof Error
          ? erro.message
          : 'Não foi possível registrar sua curtida. Tente novamente.'
      );
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
        <AppHeader
          titleAlign="center"
          showBack
          onBackPress={voltar}
          title=""
          titleOverflow="wrap"
          titleNumberOfLines={2}
        />

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
          {dados?.podeEscrever === true && (
            <PrimaryButton
              label="Escrever post"
              variant="outline"
              onPress={() => setNovoPostAberto(true)}
            />
          )}

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
                comments={post.totalComentarios ?? 0}
                onCommentsPress={() => setPostComentariosId(post.id)}
              />
            ))
          )}
        </View>
      </ScrollView>

      {dados?.podeEscrever === true && (
        <NovoPostSheet
          visible={novoPostAberto}
          clubeId={clubeId}
          onClose={() => setNovoPostAberto(false)}
          onSuccess={adicionarPost}
        />
      )}

      <ComentariosSheet
        visible={postComentariosId !== null}
        postId={postComentariosId}
        contexto="clube"
        onClose={() => setPostComentariosId(null)}
        onComentado={registrarComentario}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  tela: {
    flex: 1,
    backgroundColor: colors.surface,
  },
  tituloDoLivro: {
    fontFamily: typography.fontFamily.bold,
  },
  rolagem: {
    flexGrow: 1,
    paddingBottom: spacing[10],
  },
  conteudo: {
    padding: spacing[6],
    gap: spacing[4],
    flexGrow: 1,
  },
  carregando: {
    flexGrow: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  acoesDoErro: {
    alignSelf: 'stretch',
    gap: spacing[2],
  },
});

export default ForumClubeScreen;
