import React, { useState } from "react";
import { View, Text } from "react-native";
import { Image } from "expo-image";

/**
 * Cor do círculo-tema usada quando não há foto (fallback de inicial).
 * Vem da identidade visual do ReadRace (vinho primário).
 */
const AVATAR_BG = "#732634";
const AVATAR_FG = "#FFFFFF";
const BADGE_BG = "#FDCA7E";
const BADGE_FG = "#732634";

type AvatarProps = {
  /** Nome usado para calcular a inicial exibida quando não há foto (ou quando ela falha). */
  name?: string;
  /** URL da foto do usuário. Ausente, vazia ou que falhe ao carregar cai para a inicial. */
  photoUrl?: string | null;
  /** Diâmetro do círculo. @default 48 */
  size?: number;
  /**
   * Valor da sequência (ex.: 9 ou "9 dias"). Quando informado, exibe a pílula
   * de chama ao lado do círculo. Quando ausente, nenhum espaço é reservado.
   */
  streak?: string | number;
};

function getInitial(name?: string): string | null {
  const letter = name?.trim().charAt(0);
  return letter ? letter.toUpperCase() : null;
}

function StreakBadge({ value }: { value: string | number }) {
  return (
    <View
      className="flex-row items-center gap-1 rounded-full px-2.5 py-1"
      style={{ backgroundColor: BADGE_BG }}
    >
      <Text style={{ fontSize: 12 }}>🔥</Text>
      <Text className="text-xs font-bold" style={{ color: BADGE_FG }}>
        {value}
      </Text>
    </View>
  );
}

/**
 * Círculo de identificação do usuário (foto ou inicial do nome), com
 * pílula de sequência opcional ao lado.
 *
 * Não navega para o perfil nem carrega dados — apenas exibe o que recebe.
 */
export function Avatar({ name, photoUrl, size = 48, streak }: AvatarProps) {
  const [hasError, setHasError] = useState(false);
  const initial = getInitial(name);
  const showPhoto = !!photoUrl && !hasError;
  const hasBadge = streak !== undefined && streak !== null && streak !== "";

  return (
    <View className="flex-row items-center gap-2">
      <View
        className="items-center justify-center overflow-hidden rounded-full"
        style={{ width: size, height: size, backgroundColor: AVATAR_BG }}
      >
        {showPhoto ? (
          <Image
            source={{ uri: photoUrl! }}
            style={{ width: size, height: size }}
            contentFit="cover"
            onError={() => setHasError(true)}
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

      {hasBadge ? <StreakBadge value={streak!} /> : null}
    </View>
  );
}

export default Avatar;
