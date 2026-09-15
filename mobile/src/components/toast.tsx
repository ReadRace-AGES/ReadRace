import { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

const COPY_PADRAO = 'Funcionalidade em desenvolvimento';
const DURACAO_VISIVEL_MS = 2500;
const DURACAO_FADE_MS = 200;

type ToastProps = {
  visible: boolean;
};

export function Toast({ visible }: ToastProps) {
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

  return (
    <View
      pointerEvents="none"
      style={[styles.wrapper, { bottom: insets.bottom + 24 }]}
    >
      <Animated.View style={[styles.pill, { opacity }]}>
        {/*
          text-sm e text-white não existem mais desde o tema (#69) — o tema
          substituiu a escala padrão do Tailwind pelos tokens do projeto, e
          className é string, então o CI não acusa a classe "morta". O texto
          saía com a cor padrão (escura) em cima da pílula vinho.
        */}
        <Text className="text-bodySmall font-inter-semibold text-text-inverse">
          {COPY_PADRAO}
        </Text>
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
    paddingHorizontal: 24,
  },
  pill: {
    backgroundColor: '#732634',
    borderRadius: 999,
    paddingHorizontal: 20,
    paddingVertical: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
});

type UseToastReturn = {
  visible: boolean;
  show: () => void;
  hide: () => void;
};

/**
 * Controla o show/hide do Toast. Chamar show() de novo enquanto ja esta
 * visivel so reinicia a contagem - nunca duplica o aviso. No unmount da
 * tela o timer e limpo, entao o aviso nao sobrevive pra proxima tela.
 *
 * hide() força o toast a sumir na hora e cancela o timer pendente — é o que
 * o ToastProvider chama quando a rota muda, pra ele não ficar flutuando por
 * cima da tela seguinte.
 */
export function useToast(
  duracaoMs: number = DURACAO_VISIVEL_MS
): UseToastReturn {
  const [visible, setVisible] = useState(false);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const limparTimer = useCallback(() => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const show = useCallback(() => {
    limparTimer();
    setVisible(true);
    timeoutRef.current = setTimeout(() => {
      setVisible(false);
      timeoutRef.current = null;
    }, duracaoMs);
  }, [duracaoMs, limparTimer]);

  const hide = useCallback(() => {
    limparTimer();
    setVisible(false);
  }, [limparTimer]);

  useEffect(() => {
    return limparTimer;
  }, [limparTimer]);

  return { visible, show, hide };
}

export default Toast;