import { Image } from 'expo-image';
import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { BookIcon } from '@/components/icons/BookIcon';
import { colors, sizes } from '@/theme';

export function IconeConquista({
  uri,
  bloqueada,
  size = sizes.icon,
}: {
  uri: string | null;
  bloqueada: boolean;
  size?: number;
}) {
  const [falhou, setFalhou] = useState(false);
  return uri && !falhou ? (
    <Image
      source={{ uri }}
      style={[{ width: size, height: size }, bloqueada && styles.lockedImage]}
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
  lockedImage: { opacity: 0.5 },
});
