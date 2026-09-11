import type { ComponentType, ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { colors, radius, sizes, spacing, textStyles } from '@/theme';

export type EmptyStateIconProps = { size: number; color: string };
export type EmptyStateProps = {
  icon: ComponentType<EmptyStateIconProps>;
  message: string;
  action?: ReactNode;
};

/** A tela fornece a área livre e decide quando a lista está vazia. */
export function EmptyState({ icon: Icon, message, action }: EmptyStateProps) {
  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <View
          style={styles.circle}
          accessibilityElementsHidden
          importantForAccessibility="no-hide-descendants"
          aria-hidden
        >
          <Icon size={sizes.icon} color={colors.primarySoft} />
        </View>
        <Text style={styles.message}>{message}</Text>
        {action != null && action !== false && (
          <View style={styles.action}>{action}</View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignSelf: 'stretch',
    justifyContent: 'center',
    padding: spacing[4],
  },
  content: { alignItems: 'center', gap: spacing[6], width: '100%' },
  circle: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing[6],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
    flexShrink: 0,
  },
  message: {
    ...textStyles.body,
    color: colors.textSecondary,
    textAlign: 'center',
    alignSelf: 'stretch',
  },
  action: { alignSelf: 'stretch', alignItems: 'center' },
});
