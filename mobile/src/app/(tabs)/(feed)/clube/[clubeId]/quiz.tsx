import { useLocalSearchParams } from 'expo-router';

import { QuizScreen } from '@/features/quiz/QuizScreen';

export default function QuizRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return <QuizScreen key={clubeId} clubeId={clubeId} />;
}
