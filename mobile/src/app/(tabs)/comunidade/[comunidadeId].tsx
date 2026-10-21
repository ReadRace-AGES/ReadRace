import { useLocalSearchParams } from 'expo-router';

import { ComunidadeScreen } from '@/features/comunidade/ComunidadeScreen';

export default function ComunidadeRoute() {
  const { comunidadeId } = useLocalSearchParams<{ comunidadeId: string }>();
  return <ComunidadeScreen key={comunidadeId} comunidadeId={comunidadeId} />;
}
