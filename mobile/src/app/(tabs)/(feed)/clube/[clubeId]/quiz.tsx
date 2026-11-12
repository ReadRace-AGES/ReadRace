import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { QuizScreen } from '@/features/quiz/QuizScreen';

export default function QuizRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return (
    <AcessoClube clubeId={clubeId}>
      <QuizScreen key={clubeId} clubeId={clubeId} />
    </AcessoClube>
  );
}
