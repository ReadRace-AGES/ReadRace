import { Redirect, useRouter } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import {
  cadastroPendente,
  limparCadastro,
  registrarReenvio,
  segundosParaReenviar,
} from '@/auth/cadastroPendente';
import {
  confirmarConta,
  entrarComSenha,
  reenviarCodigo,
} from '@/auth/cognitoApi';
import {
  ErroCadastro,
  MENSAGENS_ERRO_CADASTRO,
  soDigitos,
} from '@/auth/cognitoProtocolo';
import { salvarSessao } from '@/auth/session';
import { CampoTexto } from '@/components/CampoTexto';
import { PrimaryButton } from '@/components/PrimaryButton';
import { TelaDeAcesso } from '@/components/TelaDeAcesso';
import { useVoltar } from '@/components/useVoltar';
import { colors, textStyles } from '@/theme';

/** Passo 2 do cadastro (frame `confirmar e-mail`). Chega do passo 1 ou do login sem confirmar. */
export default function ConfirmarEmailScreen() {
  const router = useRouter();
  const voltar = useVoltar('/login');
  // Lido uma vez, ao abrir: ao confirmar, o módulo é limpo antes do login, e um novo render não
  // pode cair no Redirect enquanto a sessão está sendo salva.
  const [pendente] = useState(() => cadastroPendente());
  const [ultimoEnvio, setUltimoEnvio] = useState(
    () => pendente?.ultimoEnvio ?? null
  );

  const [codigo, setCodigo] = useState('');
  const [confirmando, setConfirmando] = useState(false);
  const [erroCodigo, setErroCodigo] = useState<string | null>(null);
  const [erroGeral, setErroGeral] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [reenviandoAgora, setReenviandoAgora] = useState(false);
  const [agora, setAgora] = useState(() => Date.now());
  const enviando = useRef(false);
  const reenviando = useRef(false);

  const espera = segundosParaReenviar(ultimoEnvio, agora);

  // Relógio do "Reenviar em N s": só roda enquanto há espera.
  useEffect(() => {
    if (espera <= 0) return;
    const id = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(id);
  }, [espera]);

  // App reiniciado no meio do cadastro: não há e-mail nem senha em memória.
  if (!pendente) {
    return <Redirect href="/login" />;
  }
  const { email, senha } = pendente;

  // Confirmar com um reenvio em voo gastaria o e-mail à toa: um espera o outro.
  const podeConfirmar = codigo !== '' && !confirmando && !reenviandoAgora;
  const reenvioBloqueado = espera > 0 || confirmando || reenviandoAgora;

  async function confirmar() {
    if (!podeConfirmar || enviando.current || reenviando.current) return;
    enviando.current = true;
    setErroCodigo(null);
    setErroGeral(null);
    setAviso(null);
    setConfirmando(true);
    try {
      await confirmarConta(email, codigo);
    } catch (e) {
      const tipo = e instanceof ErroCadastro ? e.tipo : 'falha';
      // Já confirmada (ex.: pela página do Cognito): segue para o login como se fosse agora.
      if (tipo !== 'jaConfirmada') {
        if (tipo === 'codigoIncorreto') {
          setErroCodigo(MENSAGENS_ERRO_CADASTRO.codigoIncorreto);
        } else {
          setErroGeral(MENSAGENS_ERRO_CADASTRO[tipo]);
        }
        enviando.current = false;
        setConfirmando(false);
        return;
      }
    }
    // Conta confirmada: o cadastro pendente não serve mais, com ou sem login.
    limparCadastro();
    try {
      // Ao salvar, o AuthProvider percebe a sessão e o _layout leva às abas.
      await salvarSessao(await entrarComSenha(email, senha));
    } catch {
      router.dismissTo('/login');
      router.push({ pathname: '/entrar', params: { confirmada: '1' } });
    }
  }

  async function reenviar() {
    if (reenvioBloqueado || reenviando.current || enviando.current) return;
    reenviando.current = true;
    setReenviandoAgora(true);
    setErroGeral(null);
    setAviso(null);
    try {
      await reenviarCodigo(email);
      const enviadoEm = Date.now();
      // No módulo, para a espera valer se a pessoa sair e voltar ao passo 2; no estado, para a tela.
      registrarReenvio(enviadoEm);
      setUltimoEnvio(enviadoEm);
      setAgora(enviadoEm);
      setAviso('Código reenviado.');
    } catch (e) {
      const tipo = e instanceof ErroCadastro ? e.tipo : 'falha';
      setErroGeral(MENSAGENS_ERRO_CADASTRO[tipo]);
    } finally {
      reenviando.current = false;
      setReenviandoAgora(false);
    }
  }

  return (
    <TelaDeAcesso onVoltar={voltar} voltarDesabilitado={confirmando}>
      <Text style={textStyles.h3} className="mb-2 text-text">
        Confirme seu e-mail
      </Text>
      <Text style={textStyles.bodySmall} className="mb-6 text-text">
        Enviamos um código para{' '}
        <Text style={textStyles.bodySmallStrong}>{email}</Text>. Digite-o
        abaixo.
      </Text>

      <CampoTexto
        rotulo="Código"
        placeholder="Código"
        value={codigo}
        onChangeText={(texto) => {
          setCodigo(soDigitos(texto));
          setErroCodigo(null);
        }}
        editable={!confirmando}
        erro={erroCodigo}
        keyboardType="number-pad"
        autoComplete="one-time-code"
        textContentType="oneTimeCode"
        returnKeyType="go"
        onSubmitEditing={confirmar}
      />

      <Pressable
        accessibilityRole="button"
        accessibilityState={{
          disabled: reenvioBloqueado,
          busy: reenviandoAgora,
        }}
        onPress={reenviar}
        disabled={reenvioBloqueado}
        className="mt-4 self-center"
      >
        <Text
          style={[
            textStyles.bodySmall,
            {
              color: reenvioBloqueado ? colors.textMuted : colors.text,
              textDecorationLine: 'underline',
            },
          ]}
        >
          {reenviandoAgora
            ? 'Reenviando...'
            : espera > 0
              ? `Reenviar em ${espera} s`
              : 'Reenviar código'}
        </Text>
      </Pressable>

      {aviso && (
        <Text
          style={textStyles.bodySmall}
          className="mt-4 text-center text-text"
        >
          {aviso}
        </Text>
      )}
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
          label="CONFIRMAR"
          onPress={confirmar}
          disabled={!podeConfirmar}
          loading={confirmando}
        />
      </View>
    </TelaDeAcesso>
  );
}
