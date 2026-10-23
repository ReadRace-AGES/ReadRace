import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { sizes } from '@/theme';

import { Avatar, StreakBadge } from './avatar';
import { Card } from './Card';
import { CommentsIcon } from './icons/CommentsIcon';
import { HeartIcon } from './icons/HeartIcon';

// Linhas visiveis do texto recolhido, antes do "Ler mais" (`tela livro` 2011-674).
const LINHAS_RECOLHIDO = 4;

export type PostCardProps = {
  authorName: string;
  authorPhotoUrl?: string | null;
  // Opcional: sem valor, o cabecalho sai sem a pilula de chama (#22).
  authorStreak?: string | number | null;
  // Chega pronto de quem usa o componente: "3h atrás", "1d atrás".
  timeAgo: string;
  text: string;
  likes: number;
  /** Sem valor, o balão de comentários não aparece (Fórum do clube, Detalhe do livro). */
  comments?: number;
  likedByMe?: boolean;
  /** Sem handler, o coração fica só exibição (ex.: quem ainda não tem postId). */
  onLikePress?: () => void;
  /** Desabilita o toque enquanto a curtida/descurtida deste post está em andamento. */
  likeDisabled?: boolean;
  /** Sem handler, o balão fica só exibição; com ele, abre os comentários (#150). */
  onCommentsPress?: () => void;
};

/**
 * Texto do post com "Ler mais".
 *
 * Para saber se o texto passou do corte, uma copia invisivel e sem limite de
 * linhas mede a altura cheia; se ela for maior que a do texto recolhido, o link
 * aparece. `onLayout` funciona igual em Android, iOS e web - `onTextLayout` nao.
 */
function PostBody({ text }: { text: string }) {
  const [expandido, setExpandido] = useState(false);
  const [alturaCheia, setAlturaCheia] = useState(0);
  const [alturaRecolhida, setAlturaRecolhida] = useState(0);

  const cortado = !expandido && alturaCheia > alturaRecolhida + 1;

  return (
    <View className="rounded-md bg-surface-pink p-3">
      <View>
        <Text
          numberOfLines={expandido ? undefined : LINHAS_RECOLHIDO}
          onLayout={(e) => setAlturaRecolhida(e.nativeEvent.layout.height)}
          className="text-bodySmall font-inter text-text"
        >
          {text}
        </Text>
        {!expandido && (
          <View
            pointerEvents="none"
            aria-hidden
            importantForAccessibility="no-hide-descendants"
            className="absolute left-0 right-0 top-0 opacity-0"
          >
            <Text
              onLayout={(e) => setAlturaCheia(e.nativeEvent.layout.height)}
              className="text-bodySmall font-inter"
            >
              {text}
            </Text>
          </View>
        )}
      </View>
      {cortado && (
        <Pressable
          onPress={() => setExpandido(true)}
          accessibilityRole="button"
          hitSlop={8}
          className="mt-1 self-start"
        >
          <Text className="text-caption font-inter-bold text-accent">
            Ler mais
          </Text>
        </Pressable>
      )}
    </View>
  );
}

/**
 * Cartao de post do Forum do clube, do Detalhe do livro, do Feed (#22) e do
 * perfil da comunidade (#147).
 *
 * Curtir e descurtir (#101): com `onLikePress`, o coracao e o numero viram um
 * botao. Sem handler, continuam so exibicao. O balao de comentarios segue a
 * mesma regra com `comments` e `onCommentsPress` (#150).
 */
export function PostCard({
  authorName,
  authorPhotoUrl,
  authorStreak,
  timeAgo,
  text,
  likes,
  comments,
  likedByMe = false,
  onLikePress,
  likeDisabled = false,
  onCommentsPress,
}: PostCardProps) {
  const temSequencia = authorStreak != null && authorStreak !== '';
  const temTexto = text.trim() !== '';

  return (
    <Card>
      <View className="flex-row items-center gap-3">
        <Avatar
          name={authorName}
          photoUrl={authorPhotoUrl}
          size={sizes.avatar}
        />
        <View className="flex-1">
          <View className="flex-row items-center gap-2">
            <Text
              numberOfLines={1}
              className="shrink text-body font-inter-bold text-text"
            >
              {authorName}
            </Text>
            {temSequencia && <StreakBadge value={authorStreak} />}
          </View>
          <Text className="text-caption font-inter text-text-secondary">
            {timeAgo}
          </Text>
        </View>
      </View>

      {temTexto && (
        <View className="mt-3">
          <PostBody text={text} />
        </View>
      )}

      <View className="mt-3 flex-row items-center gap-4">
        {onLikePress ? (
          <Pressable
            accessibilityRole="button"
            accessibilityState={{ disabled: likeDisabled, selected: likedByMe }}
            accessibilityLabel={`${likes} curtidas${likedByMe ? ', curtido por você' : ''}`}
            disabled={likeDisabled}
            onPress={onLikePress}
            hitSlop={8}
            className="flex-row items-center gap-1"
          >
            <HeartIcon filled={likedByMe} />
            <Text className="text-bodySmall font-inter text-text-secondary">
              {likes}
            </Text>
          </Pressable>
        ) : (
          <View
            accessible
            accessibilityLabel={`${likes} curtidas${likedByMe ? ', curtido por você' : ''}`}
            className="flex-row items-center gap-1"
          >
            <HeartIcon filled={likedByMe} />
            <Text className="text-bodySmall font-inter text-text-secondary">
              {likes}
            </Text>
          </View>
        )}
        {comments !== undefined &&
          (onCommentsPress ? (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`${comments} comentários`}
              accessibilityHint="Abre os comentários"
              onPress={onCommentsPress}
              hitSlop={8}
              className="flex-row items-center gap-1"
            >
              <CommentsIcon size={sizes.iconSmall} />
              <Text className="text-bodySmall font-inter text-text-secondary">
                {comments}
              </Text>
            </Pressable>
          ) : (
            <View
              accessible
              accessibilityLabel={`${comments} comentários`}
              className="flex-row items-center gap-1"
            >
              <CommentsIcon size={sizes.iconSmall} />
              <Text className="text-bodySmall font-inter text-text-secondary">
                {comments}
              </Text>
            </View>
          ))}
      </View>
    </Card>
  );
}

export default PostCard;
