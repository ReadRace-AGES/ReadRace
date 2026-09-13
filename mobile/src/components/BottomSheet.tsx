import type { ReactNode } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import {
  Animated,
  Keyboard,
  KeyboardAvoidingView,
  Modal,
  PanResponder,
  Platform,
  Pressable,
  StyleSheet,
  View,
  useWindowDimensions,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { colors, radius, shadows, sizes, spacing } from "@/theme";

export type BottomSheetProps = {
  visible: boolean;
  onClose: () => void;
  children: ReactNode;
  accessibilityLabel?: string;
};

const ANIMATION_MS = 220;
const CLOSE_DRAG_DISTANCE = 96;
const OVERLAY_VISIBLE_OPACITY = 1;

/** Painel ancorado embaixo, com fundo escurecido, gesto de arrastar e ajuste ao teclado. */
export function BottomSheet({
  visible,
  onClose,
  children,
  accessibilityLabel = "Painel inferior",
}: BottomSheetProps) {
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const [mounted, setMounted] = useState(visible);
  const slideY = useRef(new Animated.Value(height)).current;
  const dragY = useRef(new Animated.Value(0)).current;
  const overlayOpacity = useRef(new Animated.Value(0)).current;
  const isClosing = useRef(false);

  const close = useMemo(
    () => () => {
      if (isClosing.current) return;
      isClosing.current = true;
      Keyboard.dismiss();
      onClose();
    },
    [onClose],
  );

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onMoveShouldSetPanResponder: (_, gesture) =>
          gesture.dy > 4 && Math.abs(gesture.dy) > Math.abs(gesture.dx),
        onPanResponderMove: (_, gesture) => {
          dragY.setValue(Math.max(0, gesture.dy));
        },
        onPanResponderRelease: (_, gesture) => {
          if (gesture.dy > CLOSE_DRAG_DISTANCE || gesture.vy > 1.2) {
            close();
            return;
          }

          Animated.spring(dragY, {
            toValue: 0,
            useNativeDriver: true,
            bounciness: 0,
          }).start();
        },
        onPanResponderTerminate: () => {
          Animated.spring(dragY, {
            toValue: 0,
            useNativeDriver: true,
            bounciness: 0,
          }).start();
        },
      }),
    [close, dragY],
  );

  useEffect(() => {
    if (visible) {
      isClosing.current = false;
      setMounted(true);
      dragY.setValue(0);
      slideY.setValue(height);
      Animated.parallel([
        Animated.timing(slideY, {
          toValue: 0,
          duration: ANIMATION_MS,
          useNativeDriver: true,
        }),
        Animated.timing(overlayOpacity, {
          toValue: OVERLAY_VISIBLE_OPACITY,
          duration: ANIMATION_MS,
          useNativeDriver: true,
        }),
      ]).start();
      return;
    }

    if (!mounted) return;

    Animated.parallel([
      Animated.timing(slideY, {
        toValue: height,
        duration: ANIMATION_MS,
        useNativeDriver: true,
      }),
      Animated.timing(overlayOpacity, {
        toValue: 0,
        duration: ANIMATION_MS,
        useNativeDriver: true,
      }),
    ]).start(({ finished }) => {
      if (finished) {
        dragY.setValue(0);
        setMounted(false);
        isClosing.current = false;
      }
    });
  }, [dragY, height, mounted, overlayOpacity, slideY, visible]);

  if (!mounted) return null;

  const translateY = Animated.add(slideY, dragY);
  const maxSheetHeight = Math.max(
    height - insets.top - spacing[8],
    height * 0.45,
  );

  return (
    <Modal
      animationType="none"
      transparent
      visible={mounted}
      statusBarTranslucent
      onRequestClose={close}
    >
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : "height"}
        style={styles.modalRoot}
      >
        <Animated.View
          style={[styles.overlay, { opacity: overlayOpacity }]}
          pointerEvents="auto"
        >
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Fechar painel"
            onPress={close}
            style={StyleSheet.absoluteFill}
          />
        </Animated.View>

        <Animated.View
          accessibilityViewIsModal
          accessibilityLabel={accessibilityLabel}
          style={[
            styles.sheet,
            shadows.floating,
            {
              maxHeight: maxSheetHeight,
              paddingBottom: insets.bottom + spacing[4],
              transform: [{ translateY }],
            },
          ]}
        >
          <View
            {...panResponder.panHandlers}
            accessibilityRole="adjustable"
            accessibilityLabel="Arrastar para fechar"
            style={styles.handleArea}
          >
            <View style={styles.handle} />
          </View>
          {children}
        </Animated.View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  modalRoot: {
    flex: 1,
    justifyContent: "flex-end",
  },
  overlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: colors.overlay,
  },
  sheet: {
    alignSelf: "stretch",
    borderTopLeftRadius: radius.lg,
    borderTopRightRadius: radius.lg,
    backgroundColor: colors.surface,
    paddingHorizontal: spacing[5],
    overflow: "hidden",
  },
  handleArea: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: spacing[8],
  },
  handle: {
    width: spacing[10] + spacing[2],
    height: sizes.borderWidth * 4,
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePinkStrong,
  },
});
