import { useLocalSearchParams } from 'expo-router';

import { AdicionarPerguntasScreen } from '@/features/clube/quiz/AdicionarPerguntasScreen';

export default function AdicionarPerguntasRoute() {
  const { clubeId, titulo, recompensaXp, temQuizAtivo } = useLocalSearchParams<{
    clubeId: string;
    titulo: string;
    recompensaXp: string;
    temQuizAtivo: string;
  }>();
  return (
    <AdicionarPerguntasScreen
      key={clubeId}
      clubeId={clubeId}
      titulo={titulo ?? ''}
      recompensaXp={Number(recompensaXp)}
      temQuizAtivo={temQuizAtivo === 'true'}
    />
  );
}
