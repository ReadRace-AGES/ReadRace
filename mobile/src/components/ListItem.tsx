import { Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';

import { Avatar } from '@/components/avatar';
import { BookCover } from '@/components/BookCover';
import { Card } from '@/components/Card';
import { colors, radius, sizes, spacing, textStyles } from '@/theme';

type SharedProps = {
  title: string;
  subtitle?: string;
  imageUrl?: string | null;
  onPress?: () => void;
};

export type ListItemProps = SharedProps &
  (
    | {
        variant: 'community';
        memberCount?: number;
        action?: { label: string; onPress: () => void };
        genre?: never;
        selected?: never;
      }
    | {
        variant: 'book';
        genre?: string;
        /** Omitir remove o indicador; false mostra o círculo vazio. */
        selected?: boolean;
        memberCount?: never;
        action?: never;
      }
  );

/** Linha de apresentação. A tela controla seleção, navegação e ações. */
export function ListItem(props: ListItemProps) {
  const { title, subtitle, imageUrl, onPress } = props;
  const selectable = props.variant === 'book' && props.selected !== undefined;
  const selected = props.variant === 'book' && props.selected === true;
  const metadata =
    props.variant === 'community' && props.memberCount !== undefined
      ? `${props.memberCount} membros`
      : props.variant === 'book'
        ? props.genre
        : undefined;
  const label = [title, subtitle, metadata].filter(Boolean).join(', ');
  const content = (
    <>
      <View
        accessible={false}
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants"
        style={styles.image}
      >
        {props.variant === 'community' ? (
          <Avatar name={title} photoUrl={imageUrl} size={sizes.avatarLarge} />
        ) : (
          <BookCover
            size="thumbnail"
            source={imageUrl}
            accessibilityLabel={`Capa de ${title}`}
          />
        )}
      </View>
      <View style={styles.copy}>
        <Text numberOfLines={1} ellipsizeMode="tail" style={styles.title}>
          {title}
        </Text>
        {!!subtitle && (
          <Text numberOfLines={2} style={styles.subtitle}>
            {subtitle}
          </Text>
        )}
        {!!metadata && (
          <View style={props.variant === 'book' ? styles.chip : styles.members}>
            {props.variant === 'community' && (
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
            )}
            <Text
              style={props.variant === 'book' ? styles.genre : styles.subtitle}
            >
              {metadata}
            </Text>
          </View>
        )}
      </View>
      {selectable && (
        <View style={[styles.selection, selected && styles.selectionActive]}>
          {selected && <Text style={styles.check}>✓</Text>}
        </View>
      )}
    </>
  );

  return (
    <Card surfaceStyle={[styles.surface, selected && styles.selected]}>
      <View style={styles.row}>
        {onPress ? (
          <Pressable
            accessibilityRole={selectable ? 'checkbox' : 'button'}
            accessibilityState={selectable ? { checked: selected } : undefined}
            accessibilityLabel={label}
            onPress={onPress}
            style={({ pressed }) => [styles.main, pressed && styles.pressed]}
          >
            {content}
          </Pressable>
        ) : (
          <View
            accessible
            accessibilityLabel={label}
            accessibilityState={selectable ? { selected } : undefined}
            style={styles.main}
          >
            {content}
          </View>
        )}
        {props.variant === 'community' && props.action && (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={`${props.action.label}: ${title}`}
            onPress={props.action.onPress}
            style={({ pressed }) => [styles.action, pressed && styles.pressed]}
          >
            <Text style={styles.actionLabel}>{props.action.label}</Text>
          </Pressable>
        )}
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  surface: { padding: spacing[0] },
  selected: { backgroundColor: colors.surfacePink },
  row: { flexDirection: 'row', alignItems: 'center' },
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
  members: { flexDirection: 'row', alignItems: 'center', gap: spacing[1] },
  chip: {
    alignSelf: 'flex-start',
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePinkStrong,
    paddingHorizontal: spacing[2],
    paddingVertical: spacing[1],
  },
  genre: { ...textStyles.microStrong, color: colors.primary },
  selection: {
    width: sizes.icon,
    height: sizes.icon,
    flexShrink: 0,
    borderRadius: radius.pill,
    borderWidth: sizes.borderWidth,
    borderColor: colors.borderStrong,
    alignItems: 'center',
    justifyContent: 'center',
  },
  selectionActive: {
    backgroundColor: colors.primary,
    borderColor: colors.primary,
  },
  check: { ...textStyles.bodySmallStrong, color: colors.textInverse },
  action: {
    flexShrink: 0,
    maxWidth: '40%',
    minHeight: sizes.buttonHeight,
    justifyContent: 'center',
    paddingHorizontal: spacing[3],
  },
  actionLabel: { ...textStyles.captionStrong, color: colors.accent },
  pressed: { backgroundColor: colors.surfaceAlt },
});
