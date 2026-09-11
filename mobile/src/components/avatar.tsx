import React, { useState } from "react";
import { View, Text } from "react-native";
import { Image } from "expo-image";
import Svg, { Path } from "react-native-svg";
import { colors, sizes, textStyles, typography } from "@/theme";

/**
 * Cores que o design usa mas o tema ainda nao nomeia.
 *
 * NAO transformar em token aqui: `src/theme/tokens.js` e a fonte unica e so
 * recebe valor medido num frame do designer (README, "Tema do app"). Estas duas
 * precisam de issue no tema antes de virar token — ate la ficam locais e
 * visiveis, em vez de espalhadas pelo componente:
 *
 * - a chama e `#FF383C`, o mesmo vermelho que o README ja cita ao derivar
 *   `surfacePink` (`rgba(255,56,60,.1)` sobre `#F5F5F5`) — falta so o nome;
 * - o fundo da pilula e `#EFEFEF`, vizinho de `surfaceMuted` (#F5F5F5) e de
 *   `surfaceDisabled` (#EDEDED) sem ser nenhum dos dois.
 */
const BADGE_BG_SEM_TOKEN = "#EFEFEF";
const BADGE_ICON_SEM_TOKEN = "#FF383C";

type AvatarProps = {
  name?: string;
  photoUrl?: string | null;
  /**
   * Lado do circulo em px. @default sizes.avatar (40)
   *
   * O design tem dois tamanhos: `sizes.avatar` no post, no card de desafio e na
   * linha do ranking, e `sizes.avatarLarge` no item da lista de comunidade.
   * Passe `sizes.avatarLarge` nesse caso — nao um numero solto.
   */
  size?: number;
};

export type StreakBadgeProps = {
  value: string | number;
  iconSize?: number;
};

// `charAt(0)` pega meia unidade UTF-16: em "😀Ana" sairia um caractere quebrado.
// `Array.from` itera por code point, e so letra vira inicial - emoji, digito ou
// simbolo na frente deixam o circulo vazio, como pede a issue #20.
function getInitial(name?: string): string | null {
  const [first] = Array.from(name?.trim().normalize('NFC') ?? '');
  return first && /\p{L}/u.test(first) ? first.toUpperCase() : null;
}

function FlameIcon({ height = sizes.iconSmall, color = BADGE_ICON_SEM_TOKEN }: { height?: number; color?: string }) {
  return (
    <Svg width={(height * 7) / 10} height={height} viewBox="0 0 7 10" fill="none">
      <Path
        d="M1.97149 5.47636C2.26198 5.47636 2.54056 5.36097 2.74597 5.15557C2.95137 4.95016 3.06676 4.67158 3.06676 4.38109C3.06676 3.7765 2.84771 3.50487 2.62866 3.06676C2.159 2.1279 2.53052 1.29067 3.50487 0.43811C3.72393 1.53338 4.38109 2.58484 5.25731 3.28582C6.13353 3.98679 6.57164 4.8192 6.57164 5.69542C6.57164 6.09815 6.49231 6.49694 6.33819 6.86902C6.18407 7.24109 5.95818 7.57917 5.6734 7.86395C5.38863 8.14872 5.05055 8.37462 4.67847 8.52874C4.3064 8.68286 3.90761 8.76218 3.50487 8.76218C3.10214 8.76218 2.70335 8.68286 2.33127 8.52874C1.9592 8.37462 1.62112 8.14872 1.33634 7.86395C1.05157 7.57917 0.825673 7.24109 0.671554 6.86902C0.517434 6.49694 0.43811 6.09815 0.43811 5.69542C0.43811 5.19028 0.627812 4.6904 0.876219 4.38109C0.876219 4.67158 0.991614 4.95016 1.19702 5.15557C1.40242 5.36097 1.68101 5.47636 1.97149 5.47636Z"
        stroke={color}
        strokeWidth={0.876218}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Svg>
  );
}

/**
 * Pílula de sequência (chama + valor).
 *
 * Componente separado de propósito: no Figma o badge fica ao lado do NOME, e
 * o `Avatar` não desenha nome. Quem monta a linha é a tela, que é quem conhece
 * o layout — assim a mesma dupla serve para PostCard, RankingRow e desafio,
 * cada um posicionando do seu jeito.
 *
 * ```tsx
 * <View className="flex-row items-center gap-2">
 *   <Avatar name={autor.nome} photoUrl={autor.foto} />
 *   <Text>{autor.nome}</Text>
 *   <StreakBadge value={autor.sequencia} />
 * </View>
 * ```
 */
export function StreakBadge({ value, iconSize = sizes.iconSmall }: StreakBadgeProps) {
  return (
    <View
      className="flex-row items-center gap-1 rounded-pill px-3 py-1"
      style={{ backgroundColor: BADGE_BG_SEM_TOKEN }}
    >
      <FlameIcon height={iconSize} />
      <Text style={[textStyles.bodySmallStrong, { color: colors.text }]}>{value}</Text>
    </View>
  );
}

export function Avatar({ name, photoUrl, size = sizes.avatar }: AvatarProps) {

  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  const initial = getInitial(name);
  const showPhoto = !!photoUrl && failedUrl !== photoUrl;

  return (
    <View
      className="items-center justify-center overflow-hidden rounded-pill"
      style={{ width: size, height: size, backgroundColor: colors.primary }}
    >
      {showPhoto ? (
        <Image
          source={{ uri: photoUrl! }}
          style={{ width: size, height: size }}
          contentFit="cover"
          onError={() => setFailedUrl(photoUrl!)}
        />
      ) : initial ? (
        <Text
          style={{
            // Proporcao geometrica sobre o lado do circulo, nao um tamanho de
            // fonte do design: a inicial precisa escalar junto com `size`, que
            // e quem vem do tema.
            fontFamily: typography.fontFamily.bold,
            fontSize: Math.round(size * 0.42),
            color: colors.textInverse,
          }}
        >
          {initial}
        </Text>
      ) : null}
    </View>
  );
}

export default Avatar;
