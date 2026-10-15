import { useRouter } from 'expo-router';
import { useRef, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import { guardarCadastro, ehOMesmoCadastro } from '@/auth/cadastroPendente';
import { criarConta } from '@/auth/cognitoApi';
import {
  emailValido,
  ErroCadastro,
  MENSAGENS_ERRO_CADASTRO,
  nomeValido,
  REGRA_SENHA,
  senhaValida,
  TAMANHO_MAX_NOME,
} from '@/auth/cognitoProtocolo';
import { CampoTexto } from '@/components/CampoTexto';
import { PrimaryButton } from '@/components/PrimaryButton';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { textStyles } from '@/theme';

/** Passo 1 do cadastro (frame `criar conta` do Figma). Sem seta: o Figma não desenha. */
export default function CriarContaScreen() {
  const router = useRouter();

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [criando, setCriando] = useState(false);
  const [erroEmail, setErroEmail] = useState<string | null>(null);
  const [senhaRecusada, setSenhaRecusada] = useState(false);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  // Mesmo motivo do login: o estado só muda no próximo render, a ref muda na hora.
  const enviando = useRef(false);

  const regraVermelha = (senha !== '' && !senhaValida(senha)) || senhaRecusada;
  const podeCriar =
    nomeValido(nome) && emailValido(email) && senhaValida(senha) && !criando;

  async function criar() {
    if (!podeCriar || enviando.current) return;
    // Voltou do passo 2 pela seta: a conta já existe no Cognito, um novo SignUp diria "já existe".
    if (ehOMesmoCadastro(email, senha)) {
      router.push('/confirmar-email');
      return;
    }
    enviando.current = true;
    setErroEmail(null);
    setSenhaRecusada(false);
    setErroGeral(null);
    setCriando(true);
    try {
      await criarConta(nome, email, senha);
      guardarCadastro(email, senha, Date.now());
      router.push('/confirmar-email');
    } catch (e) {
      const tipo = e instanceof ErroCadastro ? e.tipo : 'falha';
      if (tipo === 'emailJaExiste') {
        setErroEmail(MENSAGENS_ERRO_CADASTRO.emailJaExiste);
      } else if (tipo === 'senhaFraca') {
        setSenhaRecusada(true);
      } else {
        setErroGeral(MENSAGENS_ERRO_CADASTRO[tipo]);
      }
    } finally {
      enviando.current = false;
      setCriando(false);
    }
  }

  return (
    <TelaDeAcesso>
      <Text style={textStyles.h3} className="mb-6 text-text">
        Criar conta
      </Text>

      <View className="gap-6">
        <CampoTexto
          rotulo="Nome"
          placeholder="Nome"
          value={nome}
          onChangeText={setNome}
          editable={!criando}
          maxLength={TAMANHO_MAX_NOME}
          autoCapitalize="words"
          autoComplete="name"
          textContentType="name"
          returnKeyType="next"
        />
        <CampoTexto
          rotulo="Email"
          placeholder="Email"
          value={email}
          onChangeText={(texto) => {
            setEmail(texto);
            setErroEmail(null);
          }}
          editable={!criando}
          erro={erroEmail}
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
            setSenhaRecusada(false);
          }}
          editable={!criando}
          ajuda={REGRA_SENHA}
          erro={regraVermelha ? REGRA_SENHA : null}
          secureTextEntry
          autoCapitalize="none"
          autoCorrect={false}
          autoComplete="new-password"
          textContentType="newPassword"
          returnKeyType="go"
          onSubmitEditing={criar}
        />
      </View>

      {erroGeral && (
        <Text
          style={textStyles.bodySmall}
          className="mt-4 text-center text-accent"
        >
          {erroGeral}
        </Text>
      )}

      <View className="mt-8 self-center" style={{ width: '80%' }}>
        <PrimaryButton
          label="CRIAR CONTA"
          onPress={criar}
          disabled={!podeCriar}
          loading={criando}
        />
      </View>

      <View className="mt-8 items-center gap-1">
        <Text style={textStyles.bodySmall} className="text-text">
          Já tem conta?
        </Text>
        <Pressable
          accessibilityRole="link"
          onPress={() => router.dismissTo('/entrar')}
          disabled={criando}
        >
          <Text style={textStyles.bodySmallStrong} className="text-primary">
            Entrar
          </Text>
        </Pressable>
      </View>
    </TelaDeAcesso>
  );
}
