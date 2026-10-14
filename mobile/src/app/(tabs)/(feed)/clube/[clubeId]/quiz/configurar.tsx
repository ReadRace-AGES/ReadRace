import { useLocalSearchParams } from 'expo-router';

import { ConfigurarQuizScreen } from '@/features/clube/quiz/ConfigurarQuizScreen';

export default function ConfigurarQuizRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return <ConfigurarQuizScreen key={clubeId} clubeId={clubeId} />;
}