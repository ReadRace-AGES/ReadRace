import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { RankingClubeScreen } from '@/features/clube/RankingClubeScreen';

export default function RankingRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return (
    <AcessoClube clubeId={clubeId}>
      <RankingClubeScreen key={clubeId} clubeId={clubeId} />
    </AcessoClube>
  );
}
