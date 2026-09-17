import { useLocalSearchParams } from 'expo-router';

import { PaginaClubeScreen } from '@/features/clube/PaginaClubeScreen';

export default function ClubeRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return <PaginaClubeScreen key={clubeId} clubeId={clubeId} />;
}
