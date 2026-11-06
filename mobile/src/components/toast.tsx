import { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, radius, shadows, spacing } from '@/theme';

const COPY_PADRAO = 'Funcionalidade em desenvolvimento';
const DURACAO_VISIVEL_MS = 2500;
const DURACAO_FADE_MS = 200;

type ToastProps = {
  visible: boolean;
  message?: string;
  /** Altura medida da barra visível, já incluindo seu padding de área segura. */
  tabBarHeight?: number;
};

export function Toast({
  visible,
  message = COPY_PADRAO,
  tabBarHeight = 0,
}: ToastProps) {
  const opacity = useRef(new Animated.Value(0)).current;
  const [renderizado, setRenderizado] = useState(visible);
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (visible) {
      setRenderizado(true);
      Animated.timing(opacity, {
        toValue: 1,
        duration: DURACAO_FADE_MS,
        useNativeDriver: true,
      }).start();
    } else {
      Animated.timing(opacity, {
        toValue: 0,
        duration: DURACAO_FADE_MS,
        useNativeDriver: true,
      }).start(({ finished }) => {
        if (finished) setRenderizado(false);
      });
    }
  }, [visible, opacity]);

  if (!renderizado) return null;

  // Cada linha da mensagem vira um Text: o aviso de recompensa tem até uma linha por item ganho.
  const linhas = message.split('\n');
  const variasLinhas = linhas.length > 1;

  return (
    <View
      pointerEvents="none"
      style={[
        styles.wrapper,
        {
          bottom:
            (tabBarHeight > 0 ? tabBarHeight : insets.bottom) + spacing[6],
        },
      ]}
    >
      <Animated.View
        style={[
          styles.pill,
          variasLinhas && styles.bloco,
          shadows.floating,
          { opacity },
        ]}
      >
        {linhas.map((linha, indice) => (
          <Text
            key={indice}
            className="text-bodySmall font-inter-semibold text-text-inverse"
            style={variasLinhas ? styles.linhaCentralizada : undefined}
          >
            {linha}
          </Text>
        ))}
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    left: 0,
    right: 0,
    alignItems: 'center',
    paddingHorizontal: spacing[6],
  },
  pill: {
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    paddingHorizontal: spacing[5],
    paddingVertical: spacing[3],
  },
  // Com mais de uma linha, a pílula vira um bloco de cantos arredondados.
  bloco: {
    borderRadius: radius.lg,
    gap: spacing[1],
  },
  linhaCentralizada: {
    textAlign: 'center',
  },
});

type UseToastReturn = {
  visible: boolean;
  message: string;
  /** `duracaoMs` vale só para este aviso; sem ele vale a duração do hook. */
  show: (message?: string, duracaoMs?: number) => void;
  hide: () => void;
};

export function useToast(
  duracaoMs: number = DURACAO_VISIVEL_MS
): UseToastReturn {
  const [visible, setVisible] = useState(false);
  const [message, setMessage] = useState(COPY_PADRAO);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const limparTimer = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const show = useCallback(
    (mensagem?: string, duracaoDoAvisoMs?: number) => {
      limparTimer();
      setMessage(mensagem ?? COPY_PADRAO);
      setVisible(true);
      timeoutRef.current = setTimeout(() => {
        setVisible(false);
        timeoutRef.current = null;
      }, duracaoDoAvisoMs ?? duracaoMs);
    },
    [duracaoMs, limparTimer]
  );

  const hide = useCallback(() => {
    limparTimer();
    setVisible(false);
  }, [limparTimer]);

  useEffect(() => {
    return limparTimer;
  }, [limparTimer]);

  return { visible, message, show, hide };
}

export default Toast;
