import { router, useFocusEffect, type Href } from 'expo-router';
import { useCallback, useRef } from 'react';

import { useToastContext } from '@/components/toast-provider';
import { FeedComunidadesView } from '@/features/feed/FeedComunidadesView';
import { useFeedComunidades } from '@/features/feed/useFeedComunidades';

// A Pagina do clube (#35) e empilhada dentro da aba, mantendo a navbar.
function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

export default function FeedScreen() {
  const { feed, recarregar, atualizar } = useFeedComunidades();
  const { showToast } = useToastContext();

  // A aba fica montada: sem buscar de novo ao voltar, a sequência de dias de uma leitura
  // registrada em outra aba não apareceria. O primeiro foco já é coberto pela carga inicial do
  // hook.
  const primeiroFoco = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (primeiroFoco.current) {
        primeiroFoco.current = false;
        return;
      }
      atualizar();
    }, [atualizar])
  );

  return (
    <FeedComunidadesView
      feed={feed}
      recarregar={recarregar}
      atualizar={atualizar}
      onSocialPress={showToast}
      onVerTodosPress={showToast}
      onCriarGrupoPress={showToast}
      onClubePress={(clube) => router.push(rotaDoClube(clube.id))}
      onComunidadePress={showToast}
    />
  );
}
