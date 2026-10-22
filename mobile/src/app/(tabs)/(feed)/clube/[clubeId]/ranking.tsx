import { useLocalSearchParams } from 'expo-router';

import { RankingClubeScreen } from '@/features/clube/RankingClubeScreen';

export default function RankingRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return <RankingClubeScreen key={clubeId} clubeId={clubeId} />;
}
