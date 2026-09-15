import { useState, type ReactNode } from 'react';
import { Pressable, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

import { Avatar, StreakBadge } from './avatar';

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
  likedByMe?: boolean;
};

// TODO(#17): trocar pelo `Card` quando ele entrar na dev. Por enquanto so reproduz
// a moldura do `card` 403-404 para o PostCard nao ficar esperando a #17.
function CardProvisorio({ children }: { children: ReactNode }) {
  return <View className="rounded-lg border border-border bg-surface-muted p-4">{children}</View>;
}

function HeartIcon({ filled }: { filled: boolean }) {
  return (
    <Svg width={sizes.iconSmall} height={sizes.iconSmall} viewBox="0 0 24 24">
      <Path
        d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
        fill={filled ? colors.accent : 'none'}
        stroke={colors.accent}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

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
          <Text className="text-caption font-inter-bold text-accent">Ler mais</Text>
        </Pressable>
      )}
    </View>
  );
}

/**
 * Cartao de post do Forum do clube, do Detalhe do livro e do Feed (#22).
 *
 * Curtida e so exibicao (decisao do gestor de 2026-08-21): o coracao e o numero
 * nao sao tocaveis - nao e botao sem destino, e informacao.
 */
export function PostCard({
  authorName,
  authorPhotoUrl,
  authorStreak,
  timeAgo,
  text,
  likes,
  likedByMe = false,
}: PostCardProps) {
  const temSequencia = authorStreak != null && authorStreak !== '';
  const temTexto = text.trim() !== '';

  return (
    <CardProvisorio>
      <View className="flex-row items-center gap-3">
        <Avatar name={authorName} photoUrl={authorPhotoUrl} size={sizes.avatar} />
        <View className="flex-1">
          <View className="flex-row items-center gap-2">
            <Text numberOfLines={1} className="shrink text-body font-inter-bold text-text">
              {authorName}
            </Text>
            {temSequencia && <StreakBadge value={authorStreak} />}
          </View>
          <Text className="text-caption font-inter text-text-secondary">{timeAgo}</Text>
        </View>
      </View>

      {temTexto && (
        <View className="mt-3">
          <PostBody text={text} />
        </View>
      )}

      <View
        accessible
        accessibilityLabel={`${likes} curtidas${likedByMe ? ', curtido por você' : ''}`}
        className="mt-3 flex-row items-center gap-1"
      >
        <HeartIcon filled={likedByMe} />
        <Text className="text-bodySmall font-inter text-text-secondary">{likes}</Text>
      </View>
    </CardProvisorio>
  );
}

export default PostCard;
