import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useEffect } from 'react';

import { AuthProvider, useAuth } from '@/auth/AuthProvider';
import { ToastProvider } from '@/components/toast-provider';
import { colors, useAppFonts } from '@/theme';

import '../../global.css';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  return (
    <AuthProvider>
      <ToastProvider>
        <RootStack />
      </ToastProvider>
    </AuthProvider>
  );
}

function RootStack() {
  const [fontsLoaded, fontError] = useAppFonts();
  const { status } = useAuth();
  const pronto = (fontsLoaded || !!fontError) && status !== 'carregando';

  useEffect(() => {
    if (pronto) {
      SplashScreen.hideAsync();
    }
  }, [pronto]);

  if (!pronto) {
    return null;
  }

  const logado = status === 'logado';

  return (
    <Stack
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: colors.surface },
      }}
    >
      {/* Primeiro da lista: sem sessão, é a primeira tela disponível e vira o destino padrão. */}
      <Stack.Protected guard={!logado}>
        <Stack.Screen name="login" />
      </Stack.Protected>

      <Stack.Protected guard={logado}>
        <Stack.Screen name="index" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="desafiar-amigo" />
        <Stack.Screen name="escolher-livro" />
      </Stack.Protected>

      {/* Destino do redirect do OAuth: acessível com ou sem sessão. */}
      <Stack.Screen name="auth" />
    </Stack>
  );
}
