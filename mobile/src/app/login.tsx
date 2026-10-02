import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';
import { useEffect, useState } from 'react';
import { Text, View } from 'react-native';

import {
  COGNITO_CLIENT_ID,
  discovery,
  REDIRECT_PATH,
  SCHEME,
} from '@/auth/config';
import { salvarRespostaDeToken } from '@/auth/session';
import { PrimaryButton } from '@/components/PrimaryButton';
import { ReadRaceLogo } from '@/components/readracelogo';

WebBrowser.maybeCompleteAuthSession();

export default function LoginScreen() {
  const [erro, setErro] = useState<string | null>(null);
  const [entrando, setEntrando] = useState(false);

  const redirectUri = AuthSession.makeRedirectUri({
    scheme: SCHEME,
    path: REDIRECT_PATH,
  });

  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: COGNITO_CLIENT_ID,
      redirectUri,
      scopes: ['openid', 'email', 'profile'],
      usePKCE: true,
      // Sem forçar provedor: a página do Cognito mostra e-mail e senha, cadastro, código de
      // confirmação e "esqueci a senha". O Google entra como botão nessa mesma página quando
      // for configurado como provedor no Cognito.
      extraParams: { lang: 'pt-BR' },
    },
    discovery
  );

  useEffect(() => {
    // Este valor precisa estar cadastrado nas callback URLs do app client no Cognito.
    console.log('[auth] redirectUri =', redirectUri);
  }, [redirectUri]);

  useEffect(() => {
    if (response?.type === 'error') {
      setErro(
        response.params.error_description ??
          'Não foi possível entrar. Tente de novo.'
      );
      return;
    }
    if (response?.type !== 'success' || !request?.codeVerifier) {
      return;
    }

    setEntrando(true);
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
      .catch(() => setErro('Não foi possível entrar. Tente de novo.'))
      .finally(() => setEntrando(false));
  }, [response, request, redirectUri]);

  return (
    <View className="flex-1 items-center justify-center gap-8 bg-surface p-6">
      <ReadRaceLogo />
      <View className="w-full gap-3">
        <PrimaryButton
          label="Entrar"
          onPress={() => {
            setErro(null);
            promptAsync();
          }}
          disabled={!request}
          loading={entrando}
        />
        {erro && (
          <Text className="text-center text-body text-accent">{erro}</Text>
        )}
      </View>
    </View>
  );
}
