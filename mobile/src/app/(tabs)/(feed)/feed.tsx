import { router, type Href } from 'expo-router';

import { useToastContext } from '@/components/toast-provider';
import { FeedComunidadesView } from '@/features/feed/FeedComunidadesView';
import { useFeedComunidades } from '@/features/feed/useFeedComunidades';

// A Pagina do clube (#35) e empilhada dentro da aba, mantendo a navbar.
function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

export default function FeedScreen() {
  const feed = useFeedComunidades();
  const { showToast } = useToastContext();

  return (
    <FeedComunidadesView
      {...feed}
      onSocialPress={showToast}
      onVerTodosPress={showToast}
      onCriarGrupoPress={showToast}
      onClubePress={(clube) => router.push(rotaDoClube(clube.id))}
      onComunidadePress={showToast}
    />
  );
}
