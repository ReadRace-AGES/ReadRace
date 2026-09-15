import { Stack } from 'expo-router';

export const unstable_settings = { initialRouteName: 'meus-livros' };

export default function BibliotecaLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
