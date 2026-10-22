import { StatusBar } from 'expo-status-bar';
import { useEffect, useRef, type ReactNode } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';

import { VOLTAR_PATH } from '@/components/AppHeader';
import { ReadRaceLogo } from '@/components/readracelogo';
import { useAlturaTeclado } from '@/components/useAlturaTeclado';
import { colors, spacing, textStyles } from '@/theme';

type TelaDeAcessoProps = {
  children: ReactNode;
  /** Sem `onVoltar`, a seta não aparece (ex.: passo 1 do cadastro, como no Figma). */
  onVoltar?: () => void;
  voltarDesabilitado?: boolean;
};

/** Topo vinho com o logo e cartão claro: login, criar conta e confirmar e-mail. */
export function TelaDeAcesso({
  children,
  onVoltar,
  voltarDesabilitado,
}: TelaDeAcessoProps) {
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const alturaTeclado = useAlturaTeclado();
  const rolagem = useRef<ScrollView>(null);

  // Com o teclado aberto, rola até o fim para o campo e o botão ficarem acima dele.
  useEffect(() => {
    if (alturaTeclado > 0) {
      rolagem.current?.scrollToEnd({ animated: true });
    }
  }, [alturaTeclado]);

  return (
    <View style={styles.tela}>
      <StatusBar style="light" />
      <ScrollView
        ref={rolagem}
        contentContainerStyle={{
          flexGrow: 1,
          paddingTop: insets.top,
          paddingBottom: alturaTeclado,
        }}
        keyboardShouldPersistTaps="handled"
      >
        <View className="items-center gap-3 py-8">
          {onVoltar && (
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Voltar"
              onPress={onVoltar}
              disabled={voltarDesabilitado}
              hitSlop={12}
              style={styles.voltar}
            >
              <Svg width={9} height={15} viewBox="0 0 9 15" fill="none">
                <Path
                  d={VOLTAR_PATH}
                  stroke={colors.textInverse}
                  strokeWidth={2.01667}
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </Svg>
            </Pressable>
          )}
          <ReadRaceLogo size={Math.min(width * 0.36, 150)} />
          <Text style={textStyles.bodySmallStrong} className="text-logo-cream">
            Uma leitura imersiva
          </Text>
        </View>

        <View
          style={[styles.cartao, { paddingBottom: insets.bottom + spacing[8] }]}
        >
          {children}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  tela: { flex: 1, backgroundColor: colors.primary },
  cartao: {
    flexGrow: 1,
    backgroundColor: colors.surface,
    borderTopLeftRadius: 40,
    borderTopRightRadius: 40,
    paddingHorizontal: spacing[6],
    paddingTop: spacing[8],
  },
  voltar: { position: 'absolute', top: spacing[4], left: spacing[6] },
});
