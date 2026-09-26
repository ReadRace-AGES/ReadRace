import { Stack } from 'expo-router';

export const unstable_settings = { initialRouteName: 'perfil' };

export default function PerfilLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
