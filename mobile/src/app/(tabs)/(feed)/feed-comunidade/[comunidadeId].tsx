import { useLocalSearchParams } from 'expo-router';

import { ComunidadeScreen } from '@/features/comunidade/ComunidadeScreen';

// URL exclusiva do Feed: preserva a aba de origem também ao recarregar no navegador.
export default function ComunidadeRoute() {
  const { comunidadeId } = useLocalSearchParams<{ comunidadeId: string }>();
  return (
    <ComunidadeScreen
      key={comunidadeId}
      comunidadeId={comunidadeId}
      origem="/feed"
    />
  );
}
