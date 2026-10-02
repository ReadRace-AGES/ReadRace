import * as AuthSession from 'expo-auth-session';
import { StatusBar } from 'expo-status-bar';
import * as WebBrowser from 'expo-web-browser';
import { useEffect, useState } from 'react';
import { Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import {
  COGNITO_CLIENT_ID,
  discovery,
  discoveryCadastro,
  REDIRECT_PATH,
  SCHEME,
} from '@/auth/config';
import { salvarRespostaDeToken } from '@/auth/session';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ReadRaceLogo } from '@/components/readracelogo';
import { textStyles } from '@/theme';

WebBrowser.maybeCompleteAuthSession();

const ERRO_PADRAO = 'Não foi possível entrar. Tente de novo.';

/**
 * Abre a página do Cognito (login ou cadastro) e troca o código devolvido pelo token. Cada botão
 * tem o próprio request, porque o code_verifier do PKCE é de cada um.
 */
function useLoginCognito(
  redirectUri: string,
  pagina: typeof discovery,
  aoErrar: (mensagem: string) => void
) {
  const [trocando, setTrocando] = useState(false);
  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: COGNITO_CLIENT_ID,
      redirectUri,
      scopes: ['openid', 'email', 'profile'],
      usePKCE: true,
      // Sem forçar provedor: a página do Cognito mostra e-mail e senha, código de confirmação e
      // "esqueci a senha". O Google entra como botão nessa mesma página quando for configurado.
      extraParams: { lang: 'pt-BR' },
    },
    pagina
  );

  useEffect(() => {
    if (response?.type === 'error') {
      aoErrar(response.params.error_description ?? ERRO_PADRAO);
      return;
    }
    if (response?.type !== 'success' || !request?.codeVerifier) {
      return;
    }

    setTrocando(true);
    AuthSession.exchangeCodeAsync(
      {
        clientId: COGNITO_CLIENT_ID,
        code: response.params.code,
        redirectUri,
        extraParams: { code_verifier: request.codeVerifier },
      },
      discovery
    )
      // Ao salvar, o AuthProvider percebe a sessão e o _layout libera as abas.
      .then(salvarRespostaDeToken)
      .catch(() => aoErrar(ERRO_PADRAO))
      .finally(() => setTrocando(false));
    // aoErrar é um setState estável; fica fora das dependências para não refazer a troca.
  }, [response, request, redirectUri]);

  return { pronto: Boolean(request), trocando, abrir: promptAsync };
}

export default function LoginScreen() {
  const [erro, setErro] = useState<string | null>(null);
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();

  const redirectUri = AuthSession.makeRedirectUri({
    scheme: SCHEME,
    path: REDIRECT_PATH,
  });

  useEffect(() => {
    // Este valor precisa estar cadastrado nas callback URLs do app client no Cognito.
    console.log('[auth] redirectUri =', redirectUri);
  }, [redirectUri]);

  const entrar = useLoginCognito(redirectUri, discovery, setErro);
  const criarConta = useLoginCognito(redirectUri, discoveryCadastro, setErro);
  const ocupado = entrar.trocando || criarConta.trocando;

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
            onPress={() => {
              setErro(null);
              entrar.abrir();
            }}
            disabled={!entrar.pronto || criarConta.trocando}
            loading={entrar.trocando}
          />
          <PrimaryButton
            label="CRIAR CONTA"
            variant="outlineInverse"
            onPress={() => {
              setErro(null);
              criarConta.abrir();
            }}
            disabled={!criarConta.pronto || entrar.trocando}
            loading={criarConta.trocando}
          />
        </View>
        {erro && !ocupado && (
          <Text style={textStyles.body} className="px-6 text-center text-text-inverse">
            {erro}
          </Text>
        )}
      </View>
    </View>
  );
}
