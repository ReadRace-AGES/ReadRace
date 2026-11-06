import { useLocalSearchParams } from 'expo-router';

import { ForumClubeScreen } from '@/features/forum/ForumClubeScreen';

export default function ForumRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return <ForumClubeScreen key={clubeId} clubeId={clubeId} />;
}
