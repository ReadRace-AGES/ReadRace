import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { ConfigurarQuizScreen } from '@/features/clube/quiz/ConfigurarQuizScreen';

export default function ConfigurarQuizRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return (
    <AcessoClube clubeId={clubeId} administrador>
      <ConfigurarQuizScreen key={clubeId} clubeId={clubeId} />
    </AcessoClube>
  );
}
