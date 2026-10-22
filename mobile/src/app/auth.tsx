import { Redirect, useRouter } from 'expo-router';
import * as WebBrowser from 'expo-web-browser';
import { useEffect, useRef } from 'react';

import { useAuth } from '@/auth/AuthProvider';

// Destino do redirect do OAuth (readrace://auth no app, /auth no web). No web, fecha o popup do
// login e devolve o resultado para a tela de login.
WebBrowser.maybeCompleteAuthSession();

export default function AuthCallback() {
  const { status } = useAuth();
  const router = useRouter();
  const saiu = useRef(false);

  // No Android, o readrace://auth do login com Google também abre esta rota, por cima da tela de
  // login que está trocando o código pelo token. Voltar (e não ir para /login) mantém essa tela
  // viva: ao terminar, o guard do _layout leva às abas; se falhar, o erro aparece nela.
  useEffect(() => {
    if (status === 'logado' || saiu.current) return;
    saiu.current = true;
    if (router.canGoBack()) {
      router.back();
    } else {
      router.replace('/login');
    }
  }, [status, router]);

  return status === 'logado' ? <Redirect href="/" /> : null;
}
