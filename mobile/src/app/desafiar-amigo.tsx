import { Stack } from 'expo-router';

import { DesafiarAmigoScreen } from '@/features/desafios/DesafiarAmigoScreen';

export default function DesafiarAmigoRoute() {
  return (
    <>
      <Stack.Screen options={{ gestureEnabled: false }} />
      <DesafiarAmigoScreen />
    </>
  );
}
