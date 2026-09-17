import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';

import { Avatar } from '@/components/avatar';
import { Card } from '@/components/Card';
import { colors, sizes, spacing, textStyles, typography } from '@/theme';

import type { ClubeFeed } from './api';

// O ListItem (#23) tem subtitle de string simples e nao consegue destacar o titulo do livro em
// negrito e deixar o autor em regular (definicao travada da #34). Por isso o card de clube e
// composto aqui a partir do Card (#17), espelhando o visual da variante comunidade do ListItem.
function IconeMembros() {
  return (
    <View
      accessible={false}
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      <Svg
        width={sizes.iconSmall}
        height={sizes.iconSmall}
        viewBox="0 0 24 24"
        fill="none"
        stroke={colors.textSecondary}
        strokeWidth={sizes.borderWidth}
      >
        <Circle cx="9" cy="7" r="3" />
        <Path d="M3 21v-3a6 6 0 0 1 12 0v3M16 4a3 3 0 0 1 0 6M17 13a5 5 0 0 1 4 5v3" />
      </Svg>
    </View>
  );
}

export function ClubeCard({
  clube,
  onPress,
}: {
  clube: ClubeFeed;
  onPress: () => void;
}) {
  const { titulo, autor } = clube.livroAtual;
  const subtitulo = autor ? `${titulo} - ${autor}` : titulo;
  const label = `${clube.nome}, ${subtitulo}, ${clube.totalMembros} membros`;

  return (
    <Card surfaceStyle={styles.surface}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={label}
        onPress={onPress}
        style={({ pressed }) => [styles.main, pressed && styles.pressed]}
      >
        <View style={styles.image}>
          <Avatar
            name={clube.nome}
            photoUrl={clube.capaUrl}
            size={sizes.avatarLarge}
          />
        </View>
        <View style={styles.copy}>
          <Text numberOfLines={1} ellipsizeMode="tail" style={styles.title}>
            {clube.nome}
          </Text>
          <Text numberOfLines={2} style={styles.subtitle}>
            <Text style={styles.subtitleStrong}>{titulo}</Text>
            {autor ? ` - ${autor}` : ''}
          </Text>
          <View style={styles.members}>
            <IconeMembros />
            <Text style={styles.subtitle}>{clube.totalMembros} membros</Text>
          </View>
        </View>
      </Pressable>
    </Card>
  );
}

const styles = StyleSheet.create({
  surface: { padding: spacing[0] },
  main: {
    flex: 1,
    minWidth: 0,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    padding: spacing[4],
  },
  image: { flexShrink: 0 },
  copy: { flex: 1, minWidth: 0, gap: spacing[1] },
  title: { ...textStyles.bodySmallStrong, color: colors.text },
  subtitle: { ...textStyles.caption, color: colors.textSecondary },
  subtitleStrong: { fontFamily: typography.fontFamily.bold },
  members: { flexDirection: 'row', alignItems: 'center', gap: spacing[1] },
  pressed: { backgroundColor: colors.surfaceAlt },
});

export default ClubeCard;
