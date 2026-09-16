import { Tabs } from 'expo-router';

import { BottomTabBar } from '@/components/BottomTabBar';

export default function TabsLayout() {
  return (
    <Tabs
      tabBar={(props) => <BottomTabBar {...props} />}
      screenOptions={{ headerShown: false }}
    >
      <Tabs.Screen name="feed" options={{ title: 'Feed' }} />
      <Tabs.Screen name="(biblioteca)" options={{ title: 'Meus Livros' }} />
      <Tabs.Screen name="(busca)" options={{ title: 'Buscar' }} />
      <Tabs.Screen name="desafios" options={{ title: 'Desafios' }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil' }} />
      <Tabs.Screen name="outra" />
    </Tabs>
  );
}
