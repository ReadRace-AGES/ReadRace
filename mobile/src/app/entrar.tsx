import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { abrirPaginaCognito, entrarComSenha } from '@/auth/cognitoApi';
import { ErroLogin, MENSAGENS_ERRO_LOGIN } from '@/auth/cognitoProtocolo';
import { salvarSessao } from '@/auth/session';
import { CampoTexto } from '@/components/CampoTexto';
import { GoogleIcon } from '@/components/icons/GoogleIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ReadRaceLogo } from '@/components/readracelogo';
import { useToastContext } from '@/components/toast-provider';
import { colors, spacing, textStyles } from '@/theme';

/** Login com e-mail e senha (frame `cadastro` do Figma, que apesar do nome é o login). */
export default function EntrarScreen() {
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const { showToast } = useToastContext();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [entrando, setEntrando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const podeEntrar = email.trim() !== '' && senha !== '' && !entrando;

  async function entrar() {
    if (!podeEntrar) return;
    setErro(null);
    setEntrando(true);
    try {
      // Ao salvar, o AuthProvider percebe a sessão e o _layout leva às abas.
      await salvarSessao(await entrarComSenha(email, senha));
    } catch (e) {
      setErro(e instanceof ErroLogin ? e.message : MENSAGENS_ERRO_LOGIN.falha);
      setSenha('');
    } finally {
      setEntrando(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.tela}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <StatusBar style="light" />
      <ScrollView
        contentContainerStyle={{ flexGrow: 1, paddingTop: insets.top }}
        keyboardShouldPersistTaps="handled"
      >
        <View className="items-center gap-3 py-8">
          <ReadRaceLogo size={Math.min(width * 0.36, 150)} />
          <Text style={textStyles.bodySmallStrong} className="text-logo-cream">
            Uma leitura imersiva
          </Text>
        </View>

        <View style={[styles.cartao, { paddingBottom: insets.bottom + spacing[8] }]}>
          <View className="gap-6">
            <CampoTexto
              rotulo="Email"
              placeholder="Email"
              value={email}
              onChangeText={setEmail}
              editable={!entrando}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              autoComplete="email"
              textContentType="emailAddress"
              returnKeyType="next"
            />
            <CampoTexto
              rotulo="Senha"
              placeholder="Senha"
              value={senha}
              onChangeText={setSenha}
              editable={!entrando}
              secureTextEntry
              autoCapitalize="none"
              autoCorrect={false}
              autoComplete="current-password"
              textContentType="password"
              returnKeyType="go"
              onSubmitEditing={entrar}
            />
          </View>

          <Pressable
            accessibilityRole="link"
            onPress={() => abrirPaginaCognito('forgotPassword')}
            className="mt-4 self-center"
          >
            <Text style={[textStyles.bodySmall, styles.link]}>Esqueceu a senha?</Text>
          </Pressable>

          {erro && (
            <Text style={textStyles.bodySmall} className="mt-4 text-center text-accent">
              {erro}
            </Text>
          )}

          <View className="mt-8 gap-4 self-center" style={{ width: '80%' }}>
            <PrimaryButton
              label="ENTRAR"
              onPress={entrar}
              disabled={!podeEntrar}
              loading={entrando}
            />
            <PrimaryButton
              label="Continuar com Google"
              variant="outline"
              icon={GoogleIcon}
              onPress={showToast}
              disabled={entrando}
            />
          </View>

          <View className="mt-8 items-center gap-1">
            <Text style={textStyles.bodySmall} className="text-text">
              Não tem conta?
            </Text>
            <Pressable accessibilityRole="link" onPress={() => abrirPaginaCognito('signup')}>
              <Text style={textStyles.bodySmallStrong} className="text-primary">
                Criar uma
              </Text>
            </Pressable>
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
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
  link: { color: colors.text, textDecorationLine: 'underline' },
});
