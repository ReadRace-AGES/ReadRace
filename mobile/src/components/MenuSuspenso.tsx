import type { ReactNode } from 'react';
import {
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';

import { colors, radius, shadows, sizes, spacing, textStyles } from '@/theme';

export type Ancora = { x: number; y: number; width: number; height: number };

export type ItemMenuSuspenso = {
  label: string;
  icone?: ReactNode;
  destrutivo?: boolean;
  onPress: () => void;
};

type MenuSuspensoProps = {
  ancora: Ancora | null;
  itens: ItemMenuSuspenso[];
  accessibilityLabel: string;
  onClose: () => void;
};

const ALTURA_ITEM = 52;

export function MenuSuspenso({
  ancora,
  itens,
  accessibilityLabel,
  onClose,
}: MenuSuspensoProps) {
  const { width, height } = useWindowDimensions();

  if (!ancora) return null;

  const alturaMenu = itens.length * ALTURA_ITEM + spacing[2];
  const abreParaCima =
    ancora.y + ancora.height + spacing[2] + alturaMenu > height - spacing[4];
  const posicao = {
    right: Math.max(spacing[4], width - ancora.x - ancora.width),
    ...(abreParaCima
      ? { bottom: height - ancora.y + spacing[2] }
      : { top: ancora.y + ancora.height + spacing[2] }),
  };

  return (
    <Modal
      transparent
      visible
      animationType="none"
      statusBarTranslucent
      onRequestClose={onClose}
    >
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Fechar menu"
        onPress={onClose}
        style={StyleSheet.absoluteFill}
      />
      <View
        accessibilityLabel={accessibilityLabel}
        style={[styles.menu, posicao]}
      >
        {itens.map((item, indice) => (
          <Pressable
            key={item.label}
            accessibilityRole="menuitem"
            accessibilityLabel={item.label}
            onPress={() => {
              onClose();
              item.onPress();
            }}
            style={({ pressed }) => [
              styles.item,
              indice > 0 && styles.divisor,
              pressed && styles.pressionado,
            ]}
          >
            {item.icone}
            <Text
              style={[styles.texto, item.destrutivo && styles.textoDestrutivo]}
            >
              {item.label}
            </Text>
          </Pressable>
        ))}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  menu: {
    position: 'absolute',
    minWidth: 220,
    backgroundColor: colors.surface,
    borderRadius: radius.lg,
    paddingVertical: spacing[1],
    ...shadows.floating,
  },
  item: {
    minHeight: ALTURA_ITEM,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing[3],
    paddingHorizontal: spacing[4],
  },
  divisor: {
    borderTopWidth: sizes.borderWidth,
    borderTopColor: colors.border,
  },
  pressionado: { backgroundColor: colors.surfacePink },
  texto: { ...textStyles.bodyStrong, color: colors.text },
  textoDestrutivo: { color: colors.danger },
});
