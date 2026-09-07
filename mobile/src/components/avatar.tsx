import React, { useState } from "react";
import { View, Text } from "react-native";
import { Image } from "expo-image";
import Svg, { Path } from "react-native-svg";

const AVATAR_BG = "#732634";
const AVATAR_FG = "#FFFFFF";

const BADGE_BG = "#EFEFEF";
const BADGE_FG = "#1A1A1A";
const BADGE_ICON = "#FF383C";

type AvatarProps = {
  name?: string;
  photoUrl?: string | null;
  size?: number;
};

export type StreakBadgeProps = {
  value: string | number;
  iconSize?: number;
};

function getInitial(name?: string): string | null {
  const letter = name?.trim().charAt(0);
  return letter ? letter.toUpperCase() : null;
}

function FlameIcon({ height = 14, color = BADGE_ICON }: { height?: number; color?: string }) {
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
export function StreakBadge({ value, iconSize = 14 }: StreakBadgeProps) {
  return (
    <View
      className="flex-row items-center gap-1.5 rounded-full px-3 py-1.5"
      style={{ backgroundColor: BADGE_BG }}
    >
      <FlameIcon height={iconSize} />
      <Text className="text-sm font-bold" style={{ color: BADGE_FG }}>
        {value}
      </Text>
    </View>
  );
}

export function Avatar({ name, photoUrl, size = 48 }: AvatarProps) {

  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  const initial = getInitial(name);
  const showPhoto = !!photoUrl && failedUrl !== photoUrl;

  return (
    <View
      className="items-center justify-center overflow-hidden rounded-full"
      style={{ width: size, height: size, backgroundColor: AVATAR_BG }}
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
          className="font-bold"
          style={{ color: AVATAR_FG, fontSize: size * 0.42 }}
        >
          {initial}
        </Text>
      ) : null}
    </View>
  );
}

export default Avatar;
