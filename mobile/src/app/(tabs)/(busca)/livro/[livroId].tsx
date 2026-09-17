import { useLocalSearchParams } from 'expo-router';

import { LivroDetalheScreen } from '@/features/livro/LivroDetalheScreen';

export default function LivroRoute() {
  const { livroId } = useLocalSearchParams<{ livroId: string }>();
  return <LivroDetalheScreen key={livroId} livroId={livroId} />;
}
