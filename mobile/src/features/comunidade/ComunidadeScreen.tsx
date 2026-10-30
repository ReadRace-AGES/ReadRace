import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  View,
} from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';

import { ApiError } from '@/api/client';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { PostCard } from '@/components/PostCard';
import { PrimaryButton } from '@/components/PrimaryButton';
import { useToastContext } from '@/components/toast-provider';
import { ComentariosSheet } from '@/features/comentarios/ComentariosSheet';
import { tempoRelativo } from '@/features/livro/model';
import { colors } from '@/theme';

import { useComunidade } from './useComunidade';
import { styles } from './ComunidadeScreen.styles';

const COPY = {
  tentarDeNovo: 'Tentar de novo',
  semPosts: 'Esta comunidade ainda não tem postagens.',
  maisRecentes: 'Mais recentes',
  emAlta: 'Em alta',
  erroSeguimento:
    'Não foi possível atualizar sua participação. Tente novamente.',
  erroCurtida: 'Não foi possível registrar sua curtida. Tente novamente.',
} as const;

export function ComunidadeScreen({ comunidadeId }: { comunidadeId: string }) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const { showToast, showErrorToast } = useToastContext();
  const {
    comunidade,
    recarregar,
    alternarSeguimento,
    alternarCurtida,
    somarComentario,
  } = useComunidade(comunidadeId);
  const [seguimentoPendente, setSeguimentoPendente] = useState(false);
  const [curtidasPendentes, setCurtidasPendentes] = useState<Set<string>>(
    new Set()
  );
  // O id fica depois de fechar, para o painel não esvaziar enquanto desce.
  const [postDosComentarios, setPostDosComentarios] = useState<string | null>(
    null
  );
  const [comentariosAbertos, setComentariosAbertos] = useState(false);
  const dados = comunidade.situacao === 'sucesso' ? comunidade.dados : null;

  async function alternar() {
    if (seguimentoPendente || !dados) return;
    setSeguimentoPendente(true);
    try {
      await alternarSeguimento();
    } catch (erro) {
      showErrorToast(
        erro instanceof ApiError ? erro.message : COPY.erroSeguimento
      );
    } finally {
      setSeguimentoPendente(false);
    }
  }

  async function curtirPost(postId: string) {
    if (curtidasPendentes.has(postId)) return;
    setCurtidasPendentes((atual) => new Set(atual).add(postId));
    try {
      await alternarCurtida(postId);
    } catch (erro) {
      showErrorToast(
        erro instanceof ApiError ? erro.message : COPY.erroCurtida
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
    <View style={styles.tela}>
      <ScrollView contentContainerStyle={styles.rolagem}>
        <View style={[styles.hero, { paddingTop: insets.top }]}>
          {dados?.imagemUrl ? (
            <Image
              source={{ uri: dados.imagemUrl }}
              style={styles.heroImage}
              contentFit="cover"
            />
          ) : (
            <View style={styles.heroPlaceholder} />
          )}
          <View style={styles.heroShade} />
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Voltar"
            onPress={() => router.back()}
            style={styles.back}
            hitSlop={12}
          >
            <Svg width={10} height={16} viewBox="0 0 10 16" fill="none">
              <Path
                d="M8 1 2 8l6 7"
                stroke={colors.textInverse}
                strokeWidth={2}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </Svg>
          </Pressable>
        </View>
        <View style={styles.conteudo}>
          {comunidade.situacao === 'erro' ? (
            <EmptyState
              icon={BookIcon}
              message={comunidade.mensagem}
              action={
                <PrimaryButton
                  label={COPY.tentarDeNovo}
                  variant="outline"
                  onPress={recarregar}
                />
              }
            />
          ) : dados === null ? (
            <View style={styles.carregando} accessibilityLabel="Carregando">
              <ActivityIndicator color={colors.primary} />
            </View>
          ) : (
            <>
              <View style={styles.tituloLinha}>
                <Text style={styles.titulo} numberOfLines={2}>
                  {dados.nome}
                </Text>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={dados.sigo ? 'Seguindo' : 'Seguir'}
                  accessibilityState={{ disabled: seguimentoPendente }}
                  style={styles.acao}
                  onPress={alternar}
                  disabled={seguimentoPendente}
                  hitSlop={8}
                >
                  <Text style={styles.seguir}>
                    {dados.sigo ? 'Seguindo' : 'Seguir'}
                  </Text>
                </Pressable>
              </View>
              <View style={styles.resumo}>
                <View style={styles.metrica}>
                  <Text style={styles.seguidores}>{dados.seguidores}</Text>
                  <Text style={styles.rotulo}>Seguidores</Text>
                </View>
              </View>
              <Text accessibilityRole="header" style={styles.postagens}>
                Postagens
              </Text>
              <View style={styles.abas}>
                <Text style={styles.abaAtiva}>{COPY.maisRecentes}</Text>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel={COPY.emAlta}
                  onPress={showToast}
                  style={styles.acao}
                >
                  <Text style={styles.aba}>{COPY.emAlta}</Text>
                </Pressable>
              </View>
              {dados.posts.length === 0 ? (
                <EmptyState icon={BookIcon} message={COPY.semPosts} />
              ) : (
                dados.posts.map((post) => (
                  <View key={post.id} style={styles.post}>
                    <PostCard
                      authorName={post.autor.nome ?? ''}
                      authorPhotoUrl={post.autor.avatarUrl}
                      timeAgo={tempoRelativo(post.publicadoEm)}
                      text={post.texto}
                      likes={post.totalCurtidas}
                      comments={post.totalComentarios}
                      likedByMe={post.curtidoPorMim}
                      likeDisabled={curtidasPendentes.has(post.id)}
                      onLikePress={() => curtirPost(post.id)}
                      onCommentsPress={() => {
                        setPostDosComentarios(post.id);
                        setComentariosAbertos(true);
                      }}
                    />
                  </View>
                ))
              )}
            </>
          )}
        </View>
      </ScrollView>
      <ComentariosSheet
        visible={comentariosAbertos}
        postId={postDosComentarios}
        onClose={() => setComentariosAbertos(false)}
        onComentado={somarComentario}
      />
    </View>
  );
}

export default ComunidadeScreen;
