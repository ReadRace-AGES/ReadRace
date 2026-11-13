import { useLocalSearchParams, useRouter } from 'expo-router';
import { useRef, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { guardarCadastro } from '@/auth/cadastroPendente';
import {
  abrirPaginaCognito,
  entrarComGoogle,
  entrarComSenha,
} from '@/auth/cognitoApi';
import {
  emailValido,
  ErroLogin,
  MENSAGEM_ERRO_GOOGLE,
  MENSAGENS_ERRO_LOGIN,
} from '@/auth/cognitoProtocolo';
import { salvarSessao } from '@/auth/session';
import { CampoTexto } from '@/components/CampoTexto';
import { GoogleIcon } from '@/components/icons/GoogleIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { useVoltar } from '@/components/useVoltar';
import { colors, textStyles } from '@/theme';

/** Login com e-mail e senha ou Google (frame `cadastro` do Figma, que apesar do nome é o login). */
export default function EntrarScreen() {
  const router = useRouter();
  const voltar = useVoltar('/login');
  // Vindo do passo 2 quando o login automático falhou depois de confirmar (sem dado sensível).
  const { confirmada } = useLocalSearchParams<{ confirmada?: string }>();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [entrando, setEntrando] = useState(false);
  const [entrandoGoogle, setEntrandoGoogle] = useState(false);
  const [erroEmail, setErroEmail] = useState<string | null>(null);
  const [erroSenha, setErroSenha] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  // O estado `entrando` só muda no próximo render: dois toques rápidos passariam os dois. A ref
  // muda na hora. É uma só para os dois botões: um login de cada vez.
  const enviando = useRef(false);

  const ocupado = entrando || entrandoGoogle;

  async function entrar() {
    if (ocupado || enviando.current) return;
    const emailCorreto = emailValido(email);
    setErroEmail(emailCorreto ? null : 'Digite um e-mail válido.');
    setErroSenha(senha !== '' ? null : 'Digite sua senha.');
    if (!emailCorreto || senha === '') return;
    enviando.current = true;
    setErro(null);
    setEntrando(true);
    try {
      // Ao salvar, o AuthProvider percebe a sessão e o _layout leva às abas.
      await salvarSessao(await entrarComSenha(email, senha));
    } catch (e) {
      // Conta criada e nunca confirmada: leva ao código, sem reenviar (o código vale 24 h e a cota
      // de e-mails é do pool inteiro). O passo 2 tem o "Reenviar código".
      if (e instanceof ErroLogin && e.tipo === 'naoConfirmado') {
        guardarCadastro(email, senha, null);
        router.push('/confirmar-email');
        return;
      }
      setErro(e instanceof ErroLogin ? e.message : MENSAGENS_ERRO_LOGIN.falha);
      setSenha('');
    } finally {
      enviando.current = false;
      setEntrando(false);
    }
  }

  async function entrarPeloGoogle() {
    if (ocupado || enviando.current) return;
    enviando.current = true;
    setErro(null);
    setEntrandoGoogle(true);
    try {
      const sessao = await entrarComGoogle();
      // null = desistiu na escolha de conta: volta para a tela sem mensagem.
      if (sessao) {
        await salvarSessao(sessao);
      }
    } catch {
      setErro(MENSAGEM_ERRO_GOOGLE);
    } finally {
      enviando.current = false;
      setEntrandoGoogle(false);
    }
  }

  return (
    <TelaDeAcesso onVoltar={voltar} voltarDesabilitado={ocupado}>
      {confirmada === '1' && (
        <Text
          style={textStyles.bodySmall}
          className="mb-6 text-center text-text"
        >
          Conta confirmada. Entre com seu e-mail e senha.
        </Text>
      )}
      <View className="gap-6">
        <CampoTexto
          rotulo="Email"
          placeholder="Email"
          value={email}
          onChangeText={(texto) => {
            setEmail(texto);
            setErroEmail(null);
          }}
          onBlur={() =>
            setErroEmail(emailValido(email) ? null : 'Digite um e-mail válido.')
          }
          erro={erroEmail}
          editable={!ocupado}
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
          onChangeText={(texto) => {
            setSenha(texto);
            setErroSenha(null);
          }}
          erro={erroSenha}
          editable={!ocupado}
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
        <Text style={[textStyles.bodySmall, styles.link]}>
          Esqueceu a senha?
        </Text>
      </Pressable>

      {erro && (
        <Text
          style={textStyles.bodySmall}
          className="mt-4 text-center text-danger"
        >
          {erro}
        </Text>
      )}

      <View className="mt-8 gap-4 self-center" style={{ width: '80%' }}>
        <PrimaryButton
          label="ENTRAR"
          onPress={entrar}
          disabled={ocupado}
          loading={entrando}
        />
        <PrimaryButton
          label="Continuar com Google"
          variant="outline"
          icon={GoogleIcon}
          onPress={entrarPeloGoogle}
          disabled={entrando}
          loading={entrandoGoogle}
        />
      </View>

      <View className="mt-8 items-center gap-1">
        <Text style={textStyles.bodySmall} className="text-text">
          Não tem conta?
        </Text>
        <Pressable
          accessibilityRole="link"
          onPress={() => router.push('/criar-conta')}
        >
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
