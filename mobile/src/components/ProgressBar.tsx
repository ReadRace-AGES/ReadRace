import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors, sizes, radius, spacing, textStyles } from '@/theme';

export interface ProgressBarProps {
  progress?: number;
  label?: string;
  showPercent?: boolean;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  progress,
  label,
  showPercent = true,
}) => {
  const isInvalid = typeof progress !== 'number' || !Number.isFinite(progress);

  const clampedProgress = isInvalid ? 0 : Math.min(100, Math.max(0, progress));

  const formattedPercent = `${Math.round(clampedProgress)}%`;

  const hasHeader = Boolean(label) || (showPercent && !isInvalid);

  return (
    <View style={styles.container}>
      {hasHeader && (
        <View style={styles.header}>
          {Boolean(label) && <Text style={styles.label}>{label}</Text>}

          {showPercent && !isInvalid && (
            <Text style={styles.percent}>{formattedPercent}</Text>
          )}
        </View>
      )}

      <View style={styles.track}>
        <View style={[styles.fill, { width: `${clampedProgress}%` }]} />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },

  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: spacing[2],
  },

  label: {
    ...textStyles.bodySmall,
    color: colors.text,
  },

  percent: {
    ...textStyles.bodySmallStrong,
    color: colors.textSecondary,
  },

  track: {
    width: '100%',
    height: sizes.progressTrackHeight,
    borderRadius: radius.pill,
    overflow: 'hidden',
    backgroundColor: colors.progressTrack,
  },

  fill: {
    height: '100%',
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
  },
});
