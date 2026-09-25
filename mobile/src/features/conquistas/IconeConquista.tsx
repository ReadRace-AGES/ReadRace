import { Image } from 'expo-image';
import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { BookIcon } from '@/components/icons/BookIcon';
import { colors, sizes } from '@/theme';

/** Ícone da API, com o livro do tema quando a imagem falta ou não carrega. */
export function IconeConquista({
  uri,
  bloqueada,
}: {
  uri: string | null;
  bloqueada: boolean;
}) {
  const [falhou, setFalhou] = useState(false);
  return uri && !falhou ? (
    <Image
      source={{ uri }}
      style={[styles.icon, bloqueada && styles.lockedImage]}
      contentFit="contain"
      onError={() => setFalhou(true)}
    />
  ) : (
    <BookIcon
      size={sizes.icon}
      color={bloqueada ? colors.textMuted : colors.primary}
    />
  );
}

const styles = StyleSheet.create({
  icon: { width: sizes.icon, height: sizes.icon },
  // As imagens da API podem ter fundo opaco; tintColor apagaria seu conteúdo.
  lockedImage: { opacity: 0.5 },
});
