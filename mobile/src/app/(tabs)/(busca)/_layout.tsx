import { Stack } from 'expo-router';

export const unstable_settings = { initialRouteName: 'buscar' };

export default function BuscaLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
