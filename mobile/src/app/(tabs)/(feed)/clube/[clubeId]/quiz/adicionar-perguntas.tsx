import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { AdicionarPerguntasScreen } from '@/features/clube/quiz/AdicionarPerguntasScreen';

export default function AdicionarPerguntasRoute() {
  const { clubeId, titulo, recompensaXp, temQuizAtivo } = useLocalSearchParams<{
    clubeId: string;
    titulo: string;
    recompensaXp: string;
    temQuizAtivo: string;
  }>();
  return (
    <AcessoClube clubeId={clubeId} administrador>
      <AdicionarPerguntasScreen
        key={clubeId}
        clubeId={clubeId}
        titulo={titulo ?? ''}
        recompensaXp={Number(recompensaXp)}
        temQuizAtivo={temQuizAtivo === 'true'}
      />
    </AcessoClube>
  );
}
