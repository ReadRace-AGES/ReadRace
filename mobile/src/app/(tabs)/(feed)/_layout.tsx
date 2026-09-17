import { Stack } from 'expo-router';

export const unstable_settings = { initialRouteName: 'feed' };

export default function FeedLayout() {
  return <Stack screenOptions={{ headerShown: false }} />;
}
