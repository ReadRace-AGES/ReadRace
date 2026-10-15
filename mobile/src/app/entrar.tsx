import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { abrirPaginaCognito, entrarComSenha } from '@/auth/cognitoApi';
import { ErroLogin, MENSAGENS_ERRO_LOGIN } from '@/auth/cognitoProtocolo';
import { salvarSessao } from '@/auth/session';
import { CampoTexto } from '@/components/CampoTexto';
import { GoogleIcon } from '@/components/icons/GoogleIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { useToastContext } from '@/components/toast-provider';
import { useVoltar } from '@/components/useVoltar';
import { colors, textStyles } from '@/theme';

/** Login com e-mail e senha (frame `cadastro` do Figma, que apesar do nome é o login). */
export default function EntrarScreen() {
  const voltar = useVoltar('/login');
  const { showToast } = useToastContext();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [entrando, setEntrando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  // O estado `entrando` só muda no próximo render: dois toques rápidos passariam os dois. A ref
  // muda na hora.
  const enviando = useRef(false);

  const podeEntrar = email.trim() !== '' && senha !== '' && !entrando;

  async function entrar() {
    if (!podeEntrar || enviando.current) return;
    enviando.current = true;
    setErro(null);
    setEntrando(true);
    try {
      // Ao salvar, o AuthProvider percebe a sessão e o _layout leva às abas.
      await salvarSessao(await entrarComSenha(email, senha));
    } catch (e) {
      setErro(e instanceof ErroLogin ? e.message : MENSAGENS_ERRO_LOGIN.falha);
      setSenha('');
    } finally {
      enviando.current = false;
      setEntrando(false);
    }
  }

  return (
    <TelaDeAcesso onVoltar={voltar} voltarDesabilitado={entrando}>
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
    </TelaDeAcesso>
  );
}

const styles = StyleSheet.create({
  link: { color: colors.text, textDecorationLine: 'underline' },
});
