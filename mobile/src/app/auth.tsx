import { Redirect } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';

import { useAuth } from '@/auth/AuthProvider';

// Destino do redirect do OAuth (readrace://auth no app, /auth no web). No web, fecha o popup do
// login e devolve o resultado para a tela de login.
WebBrowser.maybeCompleteAuthSession();

export default function AuthCallback() {
  const { status } = useAuth();

  // O app chega aqui antes de a tela de login trocar o código pelo token. Ainda sem sessão, "/"
  // está bloqueada e o redirect não acontece; quando a sessão chega, /auth continua permitida e o
  // app ficava preso nesta tela vazia. Voltar para o login, onde a troca acontece, resolve: ao
  // terminar, o guard do _layout leva às abas, e se falhar, o erro aparece lá.
  return <Redirect href={status === 'logado' ? '/' : '/login'} />;
}
