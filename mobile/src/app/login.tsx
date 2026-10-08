import { router } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { abrirPaginaCognito } from '@/auth/cognitoApi';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ReadRaceLogo } from '@/components/readracelogo';
import { textStyles } from '@/theme';

/**
 * Tela de entrada (frame `login` do Figma): ENTRAR leva ao login do app; CRIAR CONTA abre a página
 * de cadastro do Cognito até a #157 trazer o cadastro para o app.
 */
export default function LoginScreen() {
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();

  // No frame do Figma o logo ocupa ~76% da largura e os botões ~65%.
  const tamanhoLogo = Math.min(width * 0.76, 320);

  return (
    <View
      className="flex-1 bg-primary"
      style={{ paddingTop: insets.top, paddingBottom: insets.bottom }}
    >
      <StatusBar style="light" />
      <View className="flex-1 items-center justify-center gap-6 px-6">
        <ReadRaceLogo size={tamanhoLogo} />
        <Text style={textStyles.h3} className="text-logo-cream">
          Uma leitura imersiva
        </Text>
      </View>

      <View className="items-center gap-4 pb-10">
        <View className="w-2/3 gap-4">
          <PrimaryButton
            label="ENTRAR"
            variant="inverse"
            onPress={() => router.push('/entrar')}
          />
          <PrimaryButton
            label="CRIAR CONTA"
            variant="outlineInverse"
            onPress={() => abrirPaginaCognito('signup')}
          />
        </View>
      </View>
    </View>
  );
}
