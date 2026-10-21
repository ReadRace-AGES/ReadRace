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

const AVISO_CONTA_JA_CRIADA =
  'A conta deste e-mail já foi criada. Para mudar nome ou senha, use outro e-mail.';

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
  const nomeCriado = useRef<string | null>(null);

  // Voltou do passo 2 pela seta: a conta deste e-mail já existe no Cognito, que não deixa trocar
  // nome nem senha de conta sem confirmar. Os dois ficam travados até a pessoa trocar o e-mail.
  const contaJaCriada = ehOMesmoCadastro(email, senha);
  const nomeNoCampo =
    contaJaCriada && nomeCriado.current !== null ? nomeCriado.current : nome;

  const regraVermelha = (senha !== '' && !senhaValida(senha)) || senhaRecusada;
  const podeCriar =
    nomeValido(nomeNoCampo) &&
    emailValido(email) &&
    senhaValida(senha) &&
    !criando;

  async function criar() {
    if (!podeCriar || enviando.current) return;
    // Um novo SignUp diria "já existe" para a própria conta: volta direto ao passo 2.
    if (contaJaCriada) {
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
      nomeCriado.current = nome;
      router.push('/confirmar-email');
    } catch (e) {
      const tipo = e instanceof ErroCadastro ? e.tipo : 'falha';
      if (tipo === 'emailJaExiste') {
        setErroEmail(MENSAGENS_ERRO_CADASTRO.emailJaExiste);
      } else if (tipo === 'senhaFraca') {
        // O erro é na senha: só ela se perde, como pede a issue.
        setSenha('');
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
          value={nomeNoCampo}
          onChangeText={setNome}
          editable={!criando && !contaJaCriada}
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
          ajuda={contaJaCriada ? AVISO_CONTA_JA_CRIADA : undefined}
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
          editable={!criando && !contaJaCriada}
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
