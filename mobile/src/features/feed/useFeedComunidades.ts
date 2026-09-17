import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarFeedComunidades, type FeedComunidades } from './api';

/** As duas listas chegam juntas, então a tela nunca mostra uma seção com dado parcial. */
export type FeedCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: FeedComunidades };

export type FeedComunidadesState = {
  feed: FeedCarga;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o feed de comunidades.';
}

export function useFeedComunidades(): FeedComunidadesState {
  const [feed, setFeed] = useState<FeedCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setFeed({ situacao: 'carregando' });
    buscarFeedComunidades(controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setFeed({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        setFeed({ situacao: 'erro', mensagem: mensagemDe(erro) });
      });
    return () => controller.abort();
  }, [versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);

  return { feed, recarregar };
}
