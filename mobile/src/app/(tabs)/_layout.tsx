import { Tabs } from "expo-router/js-tabs";

import { BottomTabBar } from "@/components/BottomTabBar";

export default function TabsLayout() {
  return (
    <Tabs tabBar={(props) => <BottomTabBar {...props} />} screenOptions={{ headerShown: false }}>
      <Tabs.Screen name="feed" />
      <Tabs.Screen name="meus-livros" />
      <Tabs.Screen name="buscar" />
      <Tabs.Screen name="desafios" />
      <Tabs.Screen name="perfil" />
      <Tabs.Screen name="outra" />
    </Tabs>
  );
}
