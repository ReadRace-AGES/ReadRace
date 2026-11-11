import { Tabs, useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';

import { BottomTabBar } from '@/components/BottomTabBar';
import { useToastContext } from '@/components/toast-provider';

export default function TabsLayout() {
  const [height, setHeight] = useState(0);
  const { setTabBarHeight } = useToastContext();

  // O navegador de abas continua montado sob telas da pilha raiz. Só publica a
  // medida enquanto está em foco; telas internas das abas mantêm a barra visível.
  useFocusEffect(
    useCallback(() => {
      setTabBarHeight(height);
      return () => setTabBarHeight(0);
    }, [height, setTabBarHeight])
  );

  return (
    <Tabs
      tabBar={(props) => <BottomTabBar {...props} onHeightChange={setHeight} />}
      screenOptions={{ headerShown: false }}
    >
      <Tabs.Screen name="(feed)" options={{ title: 'Feed' }} />
      <Tabs.Screen name="(biblioteca)" options={{ title: 'Meus Livros' }} />
      <Tabs.Screen name="(busca)" options={{ title: 'Buscar' }} />
      <Tabs.Screen name="desafios" options={{ title: 'Desafios' }} />
      <Tabs.Screen name="(perfil)" options={{ title: 'Perfil' }} />
    </Tabs>
  );
}
