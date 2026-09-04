import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

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
  const isInvalid =
    progress === undefined ||
    progress === null ||
    Number.isNaN(progress);

  const clampedProgress = isInvalid
    ? 0
    : Math.min(100, Math.max(0, progress));

  const formattedPercent = `${Math.round(clampedProgress)}%`;

  const hasHeader = Boolean(label) || (showPercent && !isInvalid);

  return (
    <View style={styles.container}>
      {hasHeader && (
        <View style={styles.header}>
          {Boolean(label) && (
            <Text style={styles.label}>{label}</Text>
          )}

          {showPercent && !isInvalid && (
            <Text style={styles.percent}>{formattedPercent}</Text>
          )}
        </View>
      )}

      <View style={styles.track}>
        <View
          style={[
            styles.fill,
            { width: `${clampedProgress}%` },
          ]}
        />
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
    marginBottom: 6,
  },

  label: {
    fontSize: 14,
    fontWeight: '500',
  },

  percent: {
    fontSize: 14,
    fontWeight: '600',
  },

  track: {
    width: '100%',
    height: 8,
    borderRadius: 4,
    overflow: 'hidden',
    backgroundColor: '#E5E7EB',
  },

  fill: {
    height: '100%',
    borderRadius: 4,
    backgroundColor: '#2563EB',
  },
});