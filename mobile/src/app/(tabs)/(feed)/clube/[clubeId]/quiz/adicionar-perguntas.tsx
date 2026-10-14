import { useLocalSearchParams } from 'expo-router';

import { AdicionarPerguntasScreen } from '@/features/clube/quiz/AdicionarPerguntasScreen';

export default function AdicionarPerguntasRoute() {
  const { clubeId, titulo, recompensaXp } = useLocalSearchParams<{
    clubeId: string;
    titulo: string;
    recompensaXp: string;
  }>();
  return (
    <AdicionarPerguntasScreen
      key={clubeId}
      clubeId={clubeId}
      titulo={titulo ?? ''}
      recompensaXp={Number(recompensaXp)}
    />
  );
}