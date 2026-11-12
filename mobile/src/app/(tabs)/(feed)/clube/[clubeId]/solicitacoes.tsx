import { useLocalSearchParams } from 'expo-router';

import { AcessoClube } from '@/features/clube/AcessoClube';
import { SolicitacoesClubeScreen } from '@/features/clube/SolicitacoesClubeScreen';

export default function SolicitacoesRoute() {
  const { clubeId } = useLocalSearchParams<{ clubeId: string }>();
  return (
    <AcessoClube clubeId={clubeId} administrador>
      <SolicitacoesClubeScreen key={clubeId} clubeId={clubeId} />
    </AcessoClube>
  );
}
