import { useLocalSearchParams } from 'expo-router';

import { ComunidadeScreen } from '@/features/comunidade/ComunidadeScreen';

// Mora dentro da pilha da aba para a aba continuar marcada e o voltar voltar para ela.
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
