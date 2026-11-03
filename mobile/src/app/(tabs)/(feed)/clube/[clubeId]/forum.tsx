import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { ForumClubeScreen } from '@/features/forum/ForumClubeScreen';

export default function ForumRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return (
    <AcessoClube clubeId={clubeId}>
      <ForumClubeScreen key={clubeId} clubeId={clubeId} />
    </AcessoClube>
  );
}
